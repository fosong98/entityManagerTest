package boot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Member {
    @Id
    private Long id;

    public Member() {
    }

    public Member(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
