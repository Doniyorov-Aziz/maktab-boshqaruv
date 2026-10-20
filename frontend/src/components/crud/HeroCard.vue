<template>
  <article
    class="hcard dash-card"
    :style="{ '--hc': color }"
    tabindex="0"
    data-hero-card
    @click="emit('open')"
    @keydown.enter.self="emit('open')"
  >
    <!-- 96px banner in the record's colour; the 64px tile sits on its lower edge -->
    <div class="hcard__banner" aria-hidden="true">
      <slot name="banner">
        <q-icon :name="icon" class="hcard__mark" />
      </slot>
    </div>
    <div class="hcard__tile">
      <span v-if="initials" class="hcard__initials">{{ initials }}</span>
      <IconTile
        v-else
        :icon="icon"
        size="64"
        :color="`linear-gradient(135deg, ${color}, color-mix(in srgb, ${color} 62%, black))`"
      />
    </div>
    <span
      v-if="badge"
      class="hcard__badge"
      :class="{ 'hcard__badge--glow': badge.glow }"
      :style="{ '--c': badge.color || 'var(--st-present)' }"
      data-hero-badge
      >{{ badge.label }}</span
    >

    <div class="hcard__body">
      <h3 class="hcard__title" data-hero-title>
        {{ title }}
        <q-tooltip v-if="title && title.length > 28">{{ title }}</q-tooltip>
      </h3>
      <p v-if="subtitle" class="hcard__sub">{{ subtitle }}</p>
      <slot name="meta" />

      <div v-if="$slots.kpis" class="hcard__kpis" data-hero-kpis>
        <slot name="kpis" />
      </div>
      <div v-if="$slots.bottom" class="hcard__bottom">
        <slot name="bottom" />
      </div>
    </div>

    <footer
      v-if="primary || menu.length"
      class="hcard__foot"
      @click.stop
      @keydown.enter.stop
    >
      <AppButton
        v-if="primary"
        :variant="primary.variant || 'secondary'"
        size="sm"
        :icon="primary.icon"
        :label="primary.label"
        data-hero-primary
        @click="emit('primary')"
      />
      <q-space />
      <AppButton
        v-if="menu.length"
        variant="ghost"
        size="sm"
        icon="more_horiz"
        aria-label="Boshqa amallar"
        data-hero-menu
      >
        <q-menu anchor="bottom right" self="top right" auto-close>
          <q-list dense class="hcard__menu">
            <q-item
              v-for="m in menu"
              :key="m.key"
              clickable
              :class="{ 'hcard__menu--danger': m.danger }"
              :data-menu-item="m.key"
              @click="emit('menu', m.key)"
            >
              <q-item-section avatar><q-icon :name="m.icon" /></q-item-section>
              <q-item-section>{{ m.label }}</q-item-section>
            </q-item>
          </q-list>
        </q-menu>
      </AppButton>
    </footer>
  </article>
</template>

<script setup>
// A big card for a short list (up to six records — Maktablar, Binolar): a 96px banner in the
// record's colour, a 64px icon tile (or initials) on its edge, the name (20px, two lines at
// most, the rest in a tooltip), a line under it, 3–6 numbers, a run or a bar, and at the bottom
// the main action and a "⋯" menu (edit, delete). Dashboard v3 tokens throughout.
import AppButton from '@/components/app/AppButton.vue'
import IconTile from '@/components/app/IconTile.vue'

defineProps({
  color: { type: String, default: 'var(--c-primary)' },
  icon: { type: String, default: 'school' },
  initials: { type: String, default: '' },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  // { label, color, glow }
  badge: { type: Object, default: null },
  // { label, icon, variant }
  primary: { type: Object, default: null },
  // [{ key, label, icon, danger }]
  menu: { type: Array, default: () => [] }
})
const emit = defineEmits(['open', 'primary', 'menu'])
</script>

<style scoped>
.hcard {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
}

.hcard:focus-visible {
  outline: var(--border-focus);
  outline-offset: var(--hairline);
}

.hcard__banner {
  position: relative;
  height: var(--hero-banner-h);
  overflow: hidden;
  background:
    radial-gradient(
      120% 140% at 100% 0%,
      color-mix(in srgb, white 22%, transparent) 0,
      transparent 55%
    ),
    linear-gradient(
      135deg,
      var(--hc),
      color-mix(in srgb, var(--hc) 55%, var(--c-primary))
    );
}

.hcard__mark {
  position: absolute;
  right: var(--space-4);
  bottom: calc(var(--space-6) * -1);
  color: white;
  font-size: 140px;
  opacity: 0.16;
  transform: rotate(-12deg);
}

.hcard__tile {
  position: absolute;
  top: calc(var(--hero-banner-h) - var(--tile-xl) / 2);
  left: var(--space-5);
  padding: var(--space-1);
  border-radius: calc(var(--radius-lg) + var(--space-1));
  background: var(--card-bg);
}

.hcard__initials {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--tile-xl);
  height: var(--tile-xl);
  border-radius: var(--radius-lg);
  background: linear-gradient(
    135deg,
    var(--hc),
    color-mix(in srgb, var(--hc) 62%, black)
  );
  color: white;
  font-size: var(--text-lg);
  font-weight: 800;
  letter-spacing: 0.02em;
}

.hcard__badge {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  padding: var(--hairline) var(--space-3);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--card-bg) 88%, transparent);
  color: var(--c);
  font-size: var(--text-xs);
  font-weight: 800;
}

.hcard__badge--glow {
  box-shadow:
    0 0 0 var(--hairline) color-mix(in srgb, var(--c) 45%, transparent),
    0 0 var(--space-4) color-mix(in srgb, var(--c) 55%, transparent);
}

.hcard__body {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: var(--space-2);
  padding: calc(var(--tile-xl) / 2 + var(--space-3)) var(--space-5)
    var(--space-4);
}

.hcard__title {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  color: var(--text-primary);
  font-size: var(--text-lg);
  font-weight: 700;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.hcard__sub {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  color: var(--brand-text-muted);
  font-size: var(--text-sm);
}

.hcard__kpis {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-3) var(--space-2);
  margin-top: var(--space-2);
  padding-top: var(--space-3);
  border-top: var(--hairline) solid var(--dash-edge);
}

/* a phone: two numbers a row, so their labels are not cut */
@media (max-width: 599px) {
  .hcard__kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hcard__body,
  .hcard__foot {
    padding-right: var(--space-4);
    padding-left: var(--space-4);
  }
}

.hcard__bottom {
  margin-top: var(--space-1);
}

.hcard__foot {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-5);
  border-top: var(--hairline) solid var(--dash-edge);
  cursor: default;
}

.hcard__menu {
  min-width: 180px;
}

.hcard__menu--danger {
  color: var(--st-absent);
}
</style>
