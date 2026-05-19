<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <span class="brand-icon">M</span>
        <span class="brand-text">MovieMe</span>
      </div>
      <p class="footer-desc">基于豆瓣 Top250 的电影推荐系统</p>
      <div class="footer-links">
        <router-link to="/">发现</router-link>
        <router-link to="/top250">TOP250</router-link>
        <router-link to="/boards">榜单</router-link>
        <router-link to="/charts">分类</router-link>
      </div>
      <p class="footer-copy">© 2026 MovieMe. 仅供学习使用。</p>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  margin-top: 80px;
  padding: 48px 32px;
  border-top: 1px solid var(--glass-border);
  background: var(--bg-primary);
}

.footer-inner {
  max-width: var(--content-max);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.brand-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--accent-red), #FF4D55);
  border-radius: 6px;
  font-weight: 900;
  font-size: 0.85rem;
  color: white;
}

.brand-text {
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 1rem;
  color: var(--text-primary);
}

.footer-desc {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.footer-links {
  display: flex;
  gap: 24px;
}

.footer-links a {
  font-size: 0.8rem;
  color: var(--text-secondary);
  text-decoration: none;
  transition: color var(--duration-fast) var(--ease-out);
}

.footer-links a:hover {
  color: var(--text-primary);
}

.footer-copy {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 8px;
}
</style>
