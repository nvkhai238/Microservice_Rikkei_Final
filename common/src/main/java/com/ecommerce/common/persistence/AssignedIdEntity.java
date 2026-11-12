package com.ecommerce.common.persistence;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

/**
 * Base cho entity TỰ GÁN id (UUID.randomUUID() trong constructor - DR-004).
 * <p>Vấn đề: Spring Data đoán "mới hay cũ" bằng cách xem id có null không. Id luôn khác null =>
 * nó tưởng là bản ghi cũ => gọi merge() thay vì persist(): tốn thêm 1 SELECT mỗi lần insert,
 * và merge KHÔNG cascade theo CascadeType.PERSIST => entity con (vd: dòng hàng của đơn) không được lưu.
 * <p>Giải pháp: tự trả lời isNew(). Mới tạo bằng constructor => true; sau khi persist hoặc load từ DB => false.
 */
@MappedSuperclass
public abstract class AssignedIdEntity implements Persistable<UUID> {

    @Transient
    private boolean newEntity = true;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.newEntity = false;
    }
}
