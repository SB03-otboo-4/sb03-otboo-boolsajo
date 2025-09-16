package com.sprint.otboo.clothing.entity;

import com.sprint.otboo.common.base.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 의상 속성 정의(ClothesAttributeDef) 엔티티
 *
 * <p>의상 속성(ClothesAttribute)의 정의를 관리하며, 어떤 값들을 가질 수 있는지 정보를 제공
 *
 * <ul>
 *   <li>name : 속성 이름</li>
 *   <li>selectValues : 선택 가능한 값 목록(콤마로 구분된 문자열, 선택형 속성일 경우 사용)</li>
 *   <li>attributes : 해당 정의를 가진 의상 속성 리스트(ClothesAttribute 엔티티)</li>
 *   <li>BaseEntity 상속 : id, createdAt, updatedAt 관리</li>
 * </ul>
 */
@Entity
@Table(name = "clothes_attributes_def")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ClothesAttributeDef extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "select_values", length = 255)
    private String selectValues;

    @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClothesAttribute> attributes = new ArrayList<>();

}
