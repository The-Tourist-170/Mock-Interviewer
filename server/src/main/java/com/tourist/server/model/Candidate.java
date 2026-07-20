package com.tourist.server.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Candidate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String resumePath;
    private Double score;
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> strengths;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> weaknesses;

    private String recommendationVerdict;

    @Column(columnDefinition = "TEXT")
    private String recommendationRationale;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Integer> skillRatings;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Integer> experienceBreakdown;
}