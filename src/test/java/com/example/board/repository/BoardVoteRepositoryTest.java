package com.example.board.repository;

import com.example.board.domain.board.Board;
import com.example.board.domain.board.BoardVote;
import com.example.board.domain.board.BoardVoteResponse;
import com.example.board.domain.user.User;
import com.example.board.domain.user.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BoardVoteRepositoryTest {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardVoteRepository boardVoteRepository;

    @Autowired
    private BoardVoteResponseRepository responseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("삭제되지 않은 게시글의 투표만 조회한다.")
    void findsVoteOnlyForActiveBoard() {
        User author = saveUser("작성자", "author@example.com");
        Board board = saveBoard(author, "공개 게시글");
        boardVoteRepository.save(createVote(board));

        assertThat(boardVoteRepository.findByActiveBoardId(board.getId())).isPresent();

        board.deleteBoard();
        entityManager.flush();
        entityManager.clear();

        assertThat(boardVoteRepository.findByActiveBoardId(board.getId())).isEmpty();
    }

    @Test
    @DisplayName("쓰기 락 조회는 투표 엔티티에 비관적 쓰기 락을 건다.")
    void findsVoteWithPessimisticWriteLock() {
        User author = saveUser("작성자", "lock@example.com");
        Board board = saveBoard(author, "락 대상 게시글");
        BoardVote vote = boardVoteRepository.saveAndFlush(createVote(board));
        entityManager.clear();

        BoardVote lockedVote = boardVoteRepository.findByActiveBoardIdWithWriteLock(board.getId())
                .orElseThrow();

        // 실제 영속성 컨텍스트의 LockMode를 확인하여 @Lock 선언이 적용됐는지 검증한다.
        assertThat(lockedVote.getId()).isEqualTo(vote.getId());
        assertThat(entityManager.getLockMode(lockedVote)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("사용자별 응답을 조회하고 응답 수와 왼쪽 평균을 DB에서 집계한다.")
    void findsUserResponseAndAggregatesInDatabase() {
        User author = saveUser("작성자", "aggregate-author@example.com");
        User firstVoter = saveUser("첫투표자", "first@example.com");
        User secondVoter = saveUser("둘투표자", "second@example.com");
        BoardVote vote = boardVoteRepository.save(createVote(saveBoard(author, "집계 게시글")));
        LocalDateTime responseTime = LocalDateTime.of(2026, 8, 7, 13, 0);

        BoardVoteResponse firstResponse = responseRepository.save(
                BoardVoteResponse.create(vote, firstVoter, 3, responseTime)
        );
        responseRepository.saveAndFlush(
                BoardVoteResponse.create(vote, secondVoter, 8, responseTime)
        );
        entityManager.clear();

        BoardVoteAggregateProjection aggregate = responseRepository.findAggregateByBoardVoteId(vote.getId());

        assertThat(responseRepository.findByBoardVoteIdAndVoterId(vote.getId(), firstVoter.getId()))
                .get()
                .extracting(BoardVoteResponse::getId)
                .isEqualTo(firstResponse.getId());
        assertThat(aggregate.getTotalVoteCount()).isEqualTo(2);
        assertThat(aggregate.getAverageLeftScore()).isEqualTo(5.5);
    }

    @Test
    @DisplayName("응답이 없으면 집계 건수는 0이고 평균은 null이다.")
    void returnsEmptyAggregateWithoutResponses() {
        User author = saveUser("작성자", "empty@example.com");
        BoardVote vote = boardVoteRepository.saveAndFlush(createVote(saveBoard(author, "빈 투표 게시글")));

        BoardVoteAggregateProjection aggregate = responseRepository.findAggregateByBoardVoteId(vote.getId());

        assertThat(aggregate.getTotalVoteCount()).isZero();
        assertThat(aggregate.getAverageLeftScore()).isNull();
    }

    @Test
    @DisplayName("DB 제약조건은 게시글당 하나의 투표와 사용자당 하나의 응답만 허용한다.")
    void rejectsDuplicateVoteAndResponse() {
        User author = saveUser("작성자", "unique-author@example.com");
        User voter = saveUser("투표자", "unique-voter@example.com");
        Board board = saveBoard(author, "중복 방지 게시글");
        BoardVote vote = boardVoteRepository.saveAndFlush(createVote(board));
        LocalDateTime responseTime = LocalDateTime.of(2026, 8, 7, 13, 0);
        responseRepository.saveAndFlush(BoardVoteResponse.create(vote, voter, 4, responseTime));

        assertThatThrownBy(() -> responseRepository.saveAndFlush(
                BoardVoteResponse.create(vote, voter, 6, responseTime)
        )).isInstanceOf(DataIntegrityViolationException.class);

        // 실패한 flush 이후 같은 영속성 컨텍스트를 사용할 수 없으므로 투표 중복은 별도 테스트로 검증한다.
    }

    @Test
    @DisplayName("DB 제약조건은 같은 게시글에 투표를 두 개 생성하지 못하게 한다.")
    void rejectsDuplicateVoteForBoard() {
        User author = saveUser("작성자", "unique-board@example.com");
        Board board = saveBoard(author, "투표 하나 게시글");
        boardVoteRepository.saveAndFlush(createVote(board));

        assertThatThrownBy(() -> boardVoteRepository.saveAndFlush(createVote(board)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User saveUser(String nickname, String email) {
        return userRepository.save(new User(nickname, email, "encoded-password", UserRole.USER));
    }

    private Board saveBoard(User author, String title) {
        return boardRepository.save(Board.create(author, title, "게시글 내용", List.of()));
    }

    private BoardVote createVote(Board board) {
        return BoardVote.create(
                board,
                "A 차량",
                "B 차량",
                24,
                LocalDateTime.of(2026, 8, 7, 12, 0)
        );
    }
}
