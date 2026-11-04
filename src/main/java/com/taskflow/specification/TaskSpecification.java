package com.taskflow.specification;

import org.springframework.data.jpa.domain.Specification;

import com.taskflow.entity.Task;
import com.taskflow.entity.TaskStatus;
import com.taskflow.entity.TaskPriority;

public class TaskSpecification {

    public static Specification<Task> hasStatus(TaskStatus status) {

        return (root, query, cb) ->
                status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }
    public static Specification<Task> hasPriority(TaskPriority priority) {

    return (root, query, cb) ->
            priority == null
            ? cb.conjunction()
            : cb.equal(root.get("priority"), priority);
}
public static Specification<Task> titleContains(String keyword) {

    return (root, query, cb) ->
            keyword == null || keyword.isBlank()
            ? cb.conjunction()
            : cb.like(
                    cb.lower(root.get("title")),
                    "%" + keyword.toLowerCase() + "%"
            );
}
}