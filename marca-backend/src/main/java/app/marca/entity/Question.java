package app.marca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "question")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    public enum Category {
        event, emotion, future
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private String content;

    /**
     * 场景标签。维度：time / day / season。
     * 示例：{"time": ["late_night"], "day": ["monday", "weekend"]}
     * null 或空 → 通用题，任何场景都可用作保底
     */
    @Column(columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, List<String>> tags;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
