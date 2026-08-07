package com.example.board.service;

import com.example.board.configuration.jwt.CustomUserPrincipal;
import com.example.board.domain.board.Board;
import com.example.board.domain.board.BoardModifyRecord;
import com.example.board.domain.board.BoardLikeRecord;
import com.example.board.domain.board.BoardViewRecord;
import com.example.board.domain.board.BoardVote;
import com.example.board.domain.board.BoardImage;
import com.example.board.domain.board.BoardImageKeys;
import com.example.board.domain.user.User;
import com.example.board.dto.boardDTO.request.BoardCreateRequest;
import com.example.board.dto.boardDTO.request.BoardUpdateRequest;
import com.example.board.dto.boardDTO.response.BoardAuthorResponse;
import com.example.board.dto.boardDTO.response.BoardCreateResponse;
import com.example.board.dto.boardDTO.response.BoardCursorResponse;
import com.example.board.dto.boardDTO.response.BoardDetailResponse;
import com.example.board.dto.boardDTO.response.BoardImageResponse;
import com.example.board.dto.boardDTO.response.BoardLikeResponse;
import com.example.board.dto.boardDTO.response.BoardSummaryResponse;
import com.example.board.dto.boardDTO.response.BoardUpdateResponse;
import com.example.board.exception.BadRequestException;
import com.example.board.exception.ErrorCode;
import com.example.board.exception.ForbiddenException;
import com.example.board.exception.NotFoundException;
import com.example.board.repository.BoardCommentCountProjection;
import com.example.board.repository.BoardCreatedAtProjection;
import com.example.board.repository.BoardLikeRecordRepository;
import com.example.board.repository.BoardModifyRecordRepository;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.BoardViewRecordRepository;
import com.example.board.repository.BoardVoteRepository;
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
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardModifyRecordRepository boardModifyRecordRepository;
    private final BoardViewRecordRepository boardViewRecordRepository;
    private final BoardVoteRepository boardVoteRepository;
    private final BoardLikeRecordRepository boardLikeRecordRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BoardImageStorageService boardImageStorageService;
    private final ProfileImageStorageService profileImageStorageService;

    @Transactional
    public BoardCreateResponse createBoard(BoardCreateRequest request, CustomUserPrincipal principal) {
        User author = userRepository.findByIdAndIsDeletedFalse(principal.getUserId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        List<BoardImageKeys> images = request.safeImages();
        boardImageStorageService.validateOwnedImages(author.getId(), images);

        Board board = Board.create(
                author,
                request.title().trim(),
                request.content().trim(),   //불필요한 공백 제거
                images
        );

        Board savedBoard = boardRepository.save(board);

        if (request.vote() != null) {
            // 게시글과 투표를 같은 트랜잭션에서 저장하여 둘 중 하나만 생성되는 상태를 방지한다.
            BoardVote boardVote = request.vote().toEntity(savedBoard, LocalDateTime.now());
            boardVoteRepository.save(boardVote);
        }

        return BoardCreateResponse.from(savedBoard);
    }

    public BoardCursorResponse getBoards(Long cursor, int size) {
        validateCursorRequest(cursor, size);    //파라미터 검증.

        List<Board> fetchedBoards = boardRepository.findBoardSlice(
                cursor,
                PageRequest.of(0, size + 1)     //pageable의 조건으로 넘겨 쿼리에 조건 추가. size + 1만큼 가져와(cursor가 가르키는 게시물 만큼 가져와) 다음 게시물이 추가로 있는지 확인.
        );

        boolean hasNext = fetchedBoards.size() > size;
        List<Board> boards = hasNext
                ? new ArrayList<>(fetchedBoards.subList(0, size))   //다음 게시물이 있다면 cursor가 가르키는 게시물까지 포함하여 반환.
                : fetchedBoards;

        if (boards.isEmpty()) {
            return new BoardCursorResponse(List.of(), null, false); //가져올 게시물이 없다면 빈 응답을 반환
        }

        List<Long> boardIds = boards.stream().map(Board::getId).toList();   //각 게시물의 id를 추출하여 게시물 수정 이력에서 각각 가장 최신 게시물을 조회
        Map<Long, BoardModifyRecord> latestRecordByBoardId = boardModifyRecordRepository
                .findLatestRecordsByBoardIds(boardIds)
                .stream()
                .collect(Collectors.toMap(record -> record.getBoard().getId(), Function.identity()));   //boardId, 최신 수정 이력 추출.

        Map<Long, LocalDateTime> createdAtByBoardId = boardModifyRecordRepository
                .findCreatedAtByBoardIds(boardIds)
                .stream()
                .collect(Collectors.toMap(
                        BoardCreatedAtProjection::getBoardId,
                        BoardCreatedAtProjection::getCreatedAt
                ));

        Map<Long, Long> commentCountByBoardId = commentRepository //boardId를 통해 삭제되지 않은 댓글 갯수를 Projection을 통해 집계
                .countActiveCommentsByBoardIds(boardIds)
                .stream()
                .collect(Collectors.toMap(
                        BoardCommentCountProjection::getBoardId,
                        BoardCommentCountProjection::getCommentCount
                ));

        List<BoardSummaryResponse> content = boards.stream()
                .map(board -> toSummary(
                        board,
                        requireLatestRecord(latestRecordByBoardId, board.getId()),
                        requireCreatedAt(createdAtByBoardId, board.getId()),
                        commentCountByBoardId.getOrDefault(board.getId(), 0L)
                ))
                .toList();

        Long nextCursor = hasNext ? boards.getLast().getId() : null;
        return new BoardCursorResponse(content, nextCursor, hasNext);
    }

    @Transactional
    public BoardDetailResponse getBoard(Long boardId, CustomUserPrincipal principal) {  //게시물 상세 조회
        User viewer = null;
        Board board;

        if (principal == null) {
            board = boardRepository.findActiveBoard(boardId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));
        } else {
            board = boardRepository.findActiveBoardWithWriteLock(boardId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));
            viewer = getActiveUser(principal.getUserId());
            recordViewOnce(board, viewer);  // 인증 사용자의 첫 조회만 조회수 + 1
        }

        BoardModifyRecord firstRecord = boardModifyRecordRepository
                .findFirstByBoardIdOrderByIdAsc(boardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));
        BoardModifyRecord latestRecord = boardModifyRecordRepository
                .findFirstByBoardIdOrderByIdDesc(boardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));

        boolean likedByMe = viewer != null
                && boardLikeRecordRepository.existsByLikedUserIdAndLikedBoardId(viewer.getId(), boardId);
        long commentCount = commentRepository.countByBoardIdAndIsDeletedFalse(boardId);

        return new BoardDetailResponse(
                board.getId(),
                latestRecord.getTitle(),
                latestRecord.getContent(),
                latestRecord.getBoardImage().stream().map(this::toImageResponse).toList(),
                toAuthorResponse(board.getAuthor()),
                firstRecord.getRegistDate(),
                latestRecord.getRegistDate(),
                board.getNumberOfLikes(),
                board.getNumberOfViews(),
                commentCount,
                likedByMe,
                viewer != null && board.getAuthor().getId().equals(viewer.getId())
        );
    }

    @Transactional
    public BoardUpdateResponse updateBoard(
            Long boardId,
            BoardUpdateRequest request,
            CustomUserPrincipal principal
    ) {
        List<BoardImageKeys> images = request.safeImages();
        boardImageStorageService.validateOwnedImages(principal.getUserId(), images);

        Board board = getOwnedBoardWithWriteLock(boardId, principal.getUserId());       //비관적 락을 이용한 게시글 수정
        board.addModifyRecord(
                request.title().trim(),
                request.content().trim(),
                images
        );
        return new BoardUpdateResponse(board.getId());
    }

    @Transactional
    public void deleteBoard(Long boardId, CustomUserPrincipal principal) {              //비관적 락을 이용한 게시글 삭제
        Board board = getOwnedBoardWithWriteLock(boardId, principal.getUserId());
        board.deleteBoard();
    }

    @Transactional
    public BoardLikeResponse likeBoard(Long boardId, CustomUserPrincipal principal) {   //게시글 좋아요
        Board board = getActiveBoardWithWriteLock(boardId);
        User user = getActiveUser(principal.getUserId());

        if (boardLikeRecordRepository.existsByLikedUserIdAndLikedBoardId(user.getId(), boardId)) {      //이미 좋아요 되어있으면 결과 반환.
            return new BoardLikeResponse(true, board.getNumberOfLikes());
        }

        BoardLikeRecord likeRecord = new BoardLikeRecord(user, board);      //게시물과 사용자의 좋아요 기록에 각각 사용자와 게시물을 추가.
        user.addBoardLikeRecord(likeRecord);
        board.addBoardLikeRecord(likeRecord);
        boardLikeRecordRepository.save(likeRecord);
        board.increaseNumberOfLikes();

        return new BoardLikeResponse(true, board.getNumberOfLikes());
    }

    @Transactional
    public BoardLikeResponse unlikeBoard(Long boardId, CustomUserPrincipal principal) {     //게시글 좋아요 취소
        Board board = getActiveBoardWithWriteLock(boardId);     //비관적 Lock을 이용한 조회
        User user = getActiveUser(principal.getUserId());

        return boardLikeRecordRepository.findByLikedUserIdAndLikedBoardId(user.getId(), boardId)    //사용자가 좋아요 누른 게시판 기록에서 해당 게시판을 삭제 및 게시판 좋아요한 유저에서 사용자를 삭제.
                .map(likeRecord -> {
                    user.removeBoardLikeRecord(likeRecord);
                    board.removeBoardLikeRecord(likeRecord);
                    boardLikeRecordRepository.delete(likeRecord);
                    board.decreaseNumberOfLikes();      //좋아요 수 - 1
                    return new BoardLikeResponse(false, board.getNumberOfLikes());
                })
                .orElseGet(() -> new BoardLikeResponse(false, board.getNumberOfLikes()));
    }

    private Board getOwnedBoardWithWriteLock(Long boardId, Long userId) {       //비관적 Lock을 사용해 Transaction 내에 위치해야 하지만 이 메소드는 다른 Transaction 내에서만 호출되어 따로 Transaction을 걸 필요가 없다.
        Board board = getActiveBoardWithWriteLock(boardId);
        if (!board.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException(ErrorCode.BOARD_MODIFY_FORBIDDEN);
        }
        return board;
    }

    private Board getActiveBoardWithWriteLock(Long boardId) {
        return boardRepository.findActiveBoardWithWriteLock(boardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOARD_NOT_FOUND));
    }

    private User getActiveUser(Long userId) {
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private void recordViewOnce(Board board, User viewer) {
        if (boardViewRecordRepository.existsByViewedUserIdAndViewedBoardId(viewer.getId(), board.getId())) {    //이미 조회한 경우 return
            return;
        }

        BoardViewRecord viewRecord = new BoardViewRecord(viewer, board);    //첫 조회인 경우 조회 목록에 사용자 추가.
        viewer.addBoardViewRecord(viewRecord);
        board.addBoardViewRecord(viewRecord);
        boardViewRecordRepository.save(viewRecord);
        board.increaseNumberOfViews();
    }

    private BoardSummaryResponse toSummary( //게시물 리스트 카드에 필요한 정보 반환
            Board board,
            BoardModifyRecord latestRecord,
            LocalDateTime createdAt,
            long commentCount
    ) {
        return new BoardSummaryResponse(
                board.getId(),
                latestRecord.getTitle(),
                toAuthorResponse(board.getAuthor()),
                createdAt,
                board.getNumberOfLikes(),
                commentCount,
                board.getNumberOfViews(),
                latestRecord.getBoardImage().stream()
                        .findFirst()
                        .map(BoardImage::getThumbnailObjectKey)
                        .map(boardImageStorageService::createDownloadUrl)
                        .orElse(null)
        );
    }

    private BoardImageResponse toImageResponse(BoardImage image) {
        return BoardImageResponse.from(
                image,
                boardImageStorageService.createDownloadUrl(image.getOriginalObjectKey()),
                boardImageStorageService.createDownloadUrl(image.getThumbnailObjectKey())
        );
    }

    private BoardAuthorResponse toAuthorResponse(User author) {
        // DB에는 Object Key를 유지하고 외부 응답을 만들 때만 만료 시간이 있는 조회 URL로 변환한다.
        return BoardAuthorResponse.from(
                author,
                profileImageStorageService.createDownloadUrl(author.getProfileImageObjectKey())
        );
    }

    private BoardModifyRecord requireLatestRecord(Map<Long, BoardModifyRecord> records, Long boardId) { //최신 게시글 기록 있는지 검증
        BoardModifyRecord record = records.get(boardId);
        if (record == null) {
            throw new NotFoundException(ErrorCode.BOARD_NOT_FOUND);
        }
        return record;
    }

    private LocalDateTime requireCreatedAt(Map<Long, LocalDateTime> createdAtByBoardId, Long boardId) { //작성, 수정 일자 기록 있는지 검증
        LocalDateTime createdAt = createdAtByBoardId.get(boardId);
        if (createdAt == null) {
            throw new NotFoundException(ErrorCode.BOARD_NOT_FOUND);
        }
        return createdAt;
    }

    private void validateCursorRequest(Long cursor, int size) {
        if (cursor != null && cursor <= 0) {
            throw new BadRequestException(ErrorCode.BOARD_CURSOR_INVALID);
        }
        if (size < 1 || size > 50) {
            throw new BadRequestException(ErrorCode.BOARD_PAGE_SIZE_INVALID);
        }
    }
}
