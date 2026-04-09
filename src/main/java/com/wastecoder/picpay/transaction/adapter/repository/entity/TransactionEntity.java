package com.wastecoder.picpay.transaction.adapter.repository.entity;

import com.wastecoder.picpay.common.adapter.repository.AbstractJpaPersistable;
import com.wastecoder.picpay.user.adapter.repository.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity extends AbstractJpaPersistable<Long> {

    @ManyToOne
    @JoinColumn(name = "from_user_id", nullable = false)
    private UserEntity fromUser;

    @ManyToOne
    @JoinColumn(name = "target_user_id", nullable = false)
    private UserEntity targetUser;

    @Column(name = "value")
    private BigDecimal value;

    @Column(name = "description")
    private String description;
}
