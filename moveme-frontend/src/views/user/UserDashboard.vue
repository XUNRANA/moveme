<template>
  <div class="user-dashboard">
    <!-- Hero Profile Section -->
    <section class="dashboard-hero">
      <div class="hero-bg" />
      <div class="hero-content">
        <UserProfileCard />
      </div>
    </section>

    <!-- Stats Bar -->
    <section class="dashboard-section">
      <UserStatsBar />
    </section>

    <!-- My Ratings -->
    <section class="dashboard-section">
      <UserRatingsList />
    </section>

    <!-- My Favorites -->
    <section class="dashboard-section">
      <UserFavoritesGrid />
    </section>

    <!-- Viewing History -->
    <section class="dashboard-section">
      <UserHistoryTimeline />
    </section>

    <!-- Taste Profile -->
    <section class="dashboard-section">
      <UserTasteProfile />
    </section>
  </div>
</template>

<script setup lang="ts">
import UserProfileCard from '../../components/user/UserProfileCard.vue'
import UserStatsBar from '../../components/user/UserStatsBar.vue'
import UserRatingsList from '../../components/user/UserRatingsList.vue'
import UserFavoritesGrid from '../../components/user/UserFavoritesGrid.vue'
import UserHistoryTimeline from '../../components/user/UserHistoryTimeline.vue'
import UserTasteProfile from '../../components/user/UserTasteProfile.vue'
</script>

<style scoped>
.user-dashboard {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px 80px;
}

/* Hero */
.dashboard-hero {
  position: relative;
  padding: 60px 0 40px;
  margin-bottom: 8px;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(212, 160, 23, 0.06) 0%, transparent 100%);
  border-radius: var(--radius-lg);
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 1;
}

/* Sections */
.dashboard-section {
  margin-top: 36px;
}
</style>
