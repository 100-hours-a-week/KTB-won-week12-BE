package com.example.board.service;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.domain.board.Board;
import com.example.board.domain.comment.Comment;
import com.example.board.domain.comment.CommentModifyRecord;
import com.example.board.domain.user.User;
import com.example.board.dto.commentDTO.request.CommentCreateRequest;
import com.example.board.dto.commentDTO.request.CommentUpdateRequest;
import com.example.board.dto.commentDTO.response.CommentAuthorResponse;
import com.example.board.dto.commentDTO.response.CommentCreateResponse;
import com.example.board.dto.commentDTO.response.CommentCursorResponse;
import com.example.board.dto.commentDTO.response.CommentResponse;
import com.example.board.dto.commentDTO.response.CommentUpdateResponse;
import com.example.board.exception.BadRequestException;
import com.example.board.exception.ErrorCode;
import com.example.board.exception.ForbiddenException;
import com.example.board.exception.NotFoundException;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.CommentCreatedAtProjection;
import com.example.board.repository.CommentModifyRecordRepository;
import com.example.board.repository.CommentRepository;
import com.example.board.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentModifyRecordRepository commentModifyRecordRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentCreateResponse createComment( //게시글을 비관적 락을 사용해 조회 후 댓글을 추가.
            Long boardId,
            CommentCreateRequest request,
            CustomUserPrincipal principal
    ) {
        Board board = boardRepository.findActiveBoardWithWriteLock(boardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));
        User author = getActiveUser(principal.getUserId());

        Comment comment = Comment.create(author, board, request.content().trim());
        return new CommentCreateResponse(commentRepository.save(comment).getId());
    }

    public CommentCursorResponse getComments(   //keyset curser를 이요한 무한스크롤로 댓글 조회.
            Long boardId,
            Long cursor,
            int size,
            CustomUserPrincipal principal
    ) {
        validateCursorRequest(cursor, size);    //cursor와 size가 적절한 값으로 요청되었는지 확인
        if (!boardRepository.existsByIdAndIsDeletedFalse(boardId)) {    //게시글 존재 여부 확인
            throw new NotFoundException(ErrorCode.BOARD_NOT_FOUND);
        }

        List<Comment> fetchedComments = commentRepository.findCommentSlice( //Pageable을 이용하여 0부터 size + 1 만큼 조회
                boardId,
                cursor,
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = fetchedComments.size() > size;    //cursor 존재 여부
        List<Comment> comments = hasNext
                ? new ArrayList<>(fetchedComments.subList(0, size))     //다음 댓글 존재 시 cursor 값은 제외
                : fetchedComments;  //다음 댓글이 없다면 그대로 사용

        if (comments.isEmpty()) {
            return new CommentCursorResponse(List.of(), null, false);
        }

        List<Long> commentIds = comments.stream().map(Comment::getId).toList(); //댓글들의 ID값 추출

        Map<Long, CommentModifyRecord> latestRecordByCommentId = commentModifyRecordRepository  //댓글 수정 이력 중 최신값 반환
                .findLatestRecordsByCommentIds(commentIds)
                .stream()
                .collect(Collectors.toMap(record -> record.getComment().getId(), Function.identity())); //댓글 id, CommentModifyRecord 형태로 반환

        Map<Long, LocalDateTime> createdAtByCommentId = commentModifyRecordRepository   //댓글 수정/게시 일자 반환
                .findCreatedAtByCommentIds(commentIds)
                .stream()
                .collect(Collectors.toMap(
                        CommentCreatedAtProjection::getCommentId,
                        CommentCreatedAtProjection::getCreatedAt
                )); //JPQL 집계 Projection에서 값 추출

        List<CommentResponse> content = comments.stream()
                .map(comment -> toResponse(
                        comment,
                        requireLatestRecord(latestRecordByCommentId, comment.getId()),
                        requireCreatedAt(createdAtByCommentId, comment.getId()),
                        principal == null ? null : principal.getUserId()
                ))
                .toList();

        Long nextCursor = hasNext ? comments.getLast().getId() : null;  //다음 커서값 전달
        return new CommentCursorResponse(content, nextCursor, hasNext);
    }

    @Transactional
    public CommentUpdateResponse updateComment(
            Long commentId,
            CommentUpdateRequest request,
            CustomUserPrincipal principal
    ) {
        getActiveUser(principal.getUserId());
        Comment comment = getOwnedCommentWithWriteLock(commentId, principal.getUserId());   //비관적 락을 이용한 조회 후 내용 수정
        comment.addModifyRecord(request.content().trim());
        return new CommentUpdateResponse(comment.getId());
    }

    @Transactional
    public void deleteComment(Long commentId, CustomUserPrincipal principal) {
        getActiveUser(principal.getUserId());
        Comment comment = getOwnedCommentWithWriteLock(commentId, principal.getUserId());   //비관적 락을 이용한 조회 후 삭제
        comment.deleteComment();
    }

    private Comment getOwnedCommentWithWriteLock(Long commentId, Long userId) {     //비관적 락을 이용한 댓글 조회 메소드, Transaction 내에서 사용해야함.
        Comment comment = commentRepository.findActiveCommentWithWriteLock(commentId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.COMMENT_MODIFY_FORBIDDEN);
        }
        return comment;
    }

    private User getActiveUser(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private CommentResponse toResponse(
            Comment comment,
            CommentModifyRecord latestRecord,
            LocalDateTime createdAt,
            Long requesterId
    ) {
        return new CommentResponse(
                comment.getId(),
                latestRecord.getContent(),
                CommentAuthorResponse.from(comment.getAuthor()),
                createdAt,
                latestRecord.getRegistDate(),
                comment.getAuthor().getId().equals(requesterId)
        );
    }

    private CommentModifyRecord requireLatestRecord(
            Map<Long, CommentModifyRecord> latestRecordByCommentId,
            Long commentId
    ) {
        CommentModifyRecord record = latestRecordByCommentId.get(commentId);
        if (record == null) {
            throw new NotFoundException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return record;
    }

    private LocalDateTime requireCreatedAt(Map<Long, LocalDateTime> createdAtByCommentId, Long commentId) {
        LocalDateTime createdAt = createdAtByCommentId.get(commentId);
        if (createdAt == null) {
            throw new NotFoundException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return createdAt;
    }

    private void validateCursorRequest(Long cursor, int size) { //커서 요청 검증.
        if (cursor != null && cursor <= 0) {
            throw new BadRequestException(ErrorCode.COMMENT_CURSOR_INVALID);
        }
        if (size < 1 || size > 50) {
            throw new BadRequestException(ErrorCode.COMMENT_PAGE_SIZE_INVALID);
        }
    }
}
