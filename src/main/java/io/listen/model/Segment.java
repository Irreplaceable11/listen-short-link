package io.listen.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "id_segment")
public class Segment extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String bizType;

    public Long currentMax;

    public Integer step;

    @Transient
    public Long start;

    @Transient
    public Long end;

    public Segment(Long start, Long end, Integer step) {
        this.start = start;
        this.end = end;
        this.step = step;
    }

    public Segment() {}
}
