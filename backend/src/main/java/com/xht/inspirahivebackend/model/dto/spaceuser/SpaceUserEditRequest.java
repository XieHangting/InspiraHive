package com.xht.inspirahivebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间成员编辑请求
 */
@Data
public class SpaceUserEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
