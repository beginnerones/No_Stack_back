package com.stone.microstone;

import com.stone.microstone.locallogin.domain.entity.User;
import com.stone.microstone.workbook.AnswerPDF;
import com.stone.microstone.workbook.WorkBookPDF;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Entity
@Table(name="workbook")
@Data
public class WorkBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int wb_id;

    @Column
    private String wb_title;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String wb_content;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String wb_sumtext;

    @Column
    private LocalDate wb_create;

    @Column
    private boolean wb_favorite;

    @Column(columnDefinition = "MEDIUMTEXT")
    private  String wb_answer;

    @Column
    private int wb_user_id;

    @Column
    private boolean wb_favorite_answer;

    @Column
    private String wb_title_answer;

    @ManyToOne
    @JoinColumn(name="user_id",referencedColumnName = "user_id",nullable = false)
    private User user;

    @OneToOne(mappedBy = "workBook",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private WorkBookPDF workBookPDF;

    @OneToOne(mappedBy = "workBook",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private AnswerPDF answerPDF;

}