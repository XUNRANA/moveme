import type { Directive } from 'vue'

const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible')
        observer.unobserve(entry.target)
      }
    })
  },
  { threshold: 0.1, rootMargin: '0px 0px -40px 0px' }
)

export const vAnimate: Directive = {
  mounted(el, binding) {
    const animation = binding.value || 'fade-up'
    el.classList.add('animate-on-scroll', `anim-${animation}`)
    observer.observe(el)
  },
  unmounted(el) {
    observer.unobserve(el)
  },
}
