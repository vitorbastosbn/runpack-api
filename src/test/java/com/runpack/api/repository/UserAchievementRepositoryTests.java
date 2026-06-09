package com.runpack.api.repository;

import com.runpack.api.entity.Achievement;
import com.runpack.api.entity.User;
import com.runpack.api.entity.UserAchievement;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserAchievementRepositoryTests {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserAchievementRepository repository;

    @Test
    void findByUserIdOrderByUnlockedAtDescLoadsAchievementForResponseMapping() {
        User user = new User();
        user.setEmail("runner@example.com");
        user.setName("Runner");
        user.setUsername("runner");
        user.setProvider(User.Provider.google);
        user.setProviderId("google-runner");
        entityManager.persist(user);

        Achievement achievement = new Achievement();
        achievement.setSlug("first_run");
        achievement.setName("First Run");
        achievement.setDescription("Finish your first run");
        entityManager.persist(achievement);

        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setUser(user);
        userAchievement.setAchievement(achievement);
        entityManager.persist(userAchievement);
        entityManager.flush();
        entityManager.clear();

        List<UserAchievement> achievements = repository.findByUserIdOrderByUnlockedAtDesc(user.getId());
        entityManager.clear();

        assertThat(achievements).hasSize(1);
        assertThat(achievements.get(0).getAchievement().getSlug()).isEqualTo("first_run");
    }
}
