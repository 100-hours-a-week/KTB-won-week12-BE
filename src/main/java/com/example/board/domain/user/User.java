package com.example.board.domain.user;

import com.example.board.domain.board.Board;
import com.example.board.domain.board.BoardLikeRecord;
import com.example.board.domain.board.BoardReportRecord;
import com.example.board.domain.board.BoardViewRecord;
import com.example.board.domain.comment.CommentLikeRecord;
import com.example.board.domain.comment.CommentReportRecord;
import com.example.board.exception.errorMessage.UserErrorMessage;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "users")  //h2예약어 피하기 위해 적용
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;        //사용자 식별용 id값, UUID 사용
    private String nickname;    //닉네임
    private String email;       //로그인 아이디
    private String password;    //비밀번호(일단 평문으로 저장)
    @Enumerated(EnumType.STRING)
    private UserRole userRole;  //사용자 권한 구분(사용자, 어드민)
    private Boolean isDeleted;  //사용자 탈퇴 여부(소프트 delete)
    private String deleteReason; //사용자 탈퇴 사유
    // 만료되는 조회 URL이 아니라 S3 객체를 영구 식별하는 Object Key를 저장한다.
    @Column(name = "profile_image_object_key", length = 512)
    private String profileImageObjectKey;
    @OneToMany(mappedBy = "author")
    private List<Board> boardList = new ArrayList<>();

    @OneToMany(mappedBy = "viewedUser")
    private List<BoardViewRecord> boardViewRecords = new ArrayList<>();

    @OneToMany(mappedBy = "likedUser")
    private List<BoardLikeRecord> boardLikeRecords = new ArrayList<>();

    @OneToMany(mappedBy = "reportedUser")
    private List<BoardReportRecord> boardReportRecords = new ArrayList<>();

    @OneToMany(mappedBy = "likedUser")
    private List<CommentLikeRecord> commentLikeRecords = new ArrayList<>();

    @OneToMany(mappedBy = "reportedUser")
    private List<CommentReportRecord> commentReportRecords = new ArrayList<>();

    protected User(){}

    public User(String nickname, String email, String password, UserRole userRole){
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.userRole = userRole;
        this.isDeleted = false;
        this.deleteReason = "";
        // 회원가입 시에는 프로필 이미지를 받지 않고 로그인 후 별도 수정 흐름에서만 설정
        this.profileImageObjectKey = null;
    }

    public void changeNickname(String nickname){
        validateNickname(nickname);
        this.nickname = nickname;
    }

    public void changeEncodedPassword(String encodedPassword){
        this.password = encodedPassword;
    }

    public void changeProfileImageObjectKey(String profileImageObjectKey){
        this.profileImageObjectKey = profileImageObjectKey;
    }

    public void deleteUser(String deleteReason){
        this.isDeleted = true;
        this.deleteReason = deleteReason;
    }

    public void validateNickname(String nickname){
        if(nickname == null || nickname.isBlank()){
            throw new IllegalArgumentException(UserErrorMessage.NICKNAME_REQUIRED);
        }

        if(nickname.length() < 2 || nickname.length() > 10){
            throw new IllegalArgumentException(UserErrorMessage.NICKNAME_LENGTH_LIMIT);
        }
    }

    public void addBoard(Board board){
        this.boardList.add(board);
    }

    public void addBoardViewRecord(BoardViewRecord boardViewRecord){
        this.boardViewRecords.add(boardViewRecord);
    }

    public void addBoardLikeRecord(BoardLikeRecord boardLikeRecord){
        this.boardLikeRecords.add(boardLikeRecord);
    }

    public void removeBoardLikeRecord(BoardLikeRecord boardLikeRecord){
        this.boardLikeRecords.remove(boardLikeRecord);
    }

    public void addBoardReportRecord(BoardReportRecord boardReportRecord){
        this.boardReportRecords.add(boardReportRecord);
    }

    public void removeBoardReportRecord(BoardReportRecord boardReportRecord){
        this.boardReportRecords.remove(boardReportRecord);
    }

    public void addCommentLikeRecord(CommentLikeRecord commentLikeRecord){
        this.commentLikeRecords.add(commentLikeRecord);
    }

    public void removeCommentLikeRecord(CommentLikeRecord commentLikeRecord){
        this.commentLikeRecords.remove(commentLikeRecord);
    }

    public void addCommentReportedRecord(CommentReportRecord commentReportRecord){
        this.commentReportRecords.add(commentReportRecord);
    }

    public void removeCommentReportedRecord(CommentReportRecord commentReportRecord){
        this.commentReportRecords.remove(commentReportRecord);
    }
}
