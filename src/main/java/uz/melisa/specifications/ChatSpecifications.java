package uz.melisa.specifications;

import org.springframework.data.jpa.domain.Specification;
import uz.melisa.domain.Chat;

public final class ChatSpecifications {

    private ChatSpecifications() {
    }

    public static Specification<Chat> byUser(long userId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("userId"), userId),
                cb.isFalse(root.get("isDeleted"))
        );
    }
}
