package com.group17.game.Models;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "security_answers")
public class SecurityAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "user_id")
    private long userID;

    @NotBlank
    @Column(name = "question")
    private String question;

    @NotBlank
    @Column(name = "answer_hash")
    private String answerHash;

    @Column(name = "created")
    private Date created;

    
    public SecurityAnswer() {}

    public SecurityAnswer(long userID, String question, String answerHash) {
        this.userID = userID;
        this.question = question;
        this.answerHash = answerHash;
        this.created = new Date();
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userID;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswerHash() {
        return answerHash;
    }

    public Date getCreated() {
        return created;
    }
}
