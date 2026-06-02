<template>
  <img
    :src="dataUri"
    :alt="name || 'avatar'"
    :title="name"
    :class="['flex-shrink-0 rounded-full object-cover', sizeClass]"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { createAvatar } from '@dicebear/core';
import {
  adventurer,
  avataaars,
  bigSmile,
  bottts,
  croodles,
  funEmoji,
  identicon,
  lorelei,
  micah,
  miniavs,
  notionists,
  openPeeps,
  personas,
  pixelArt,
  thumbs
} from '@dicebear/collection';
import type { Style } from '@dicebear/core';

interface Props {
  name?: string;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  seed?: string;
}

const props = withDefaults(defineProps<Props>(), {
  name: '',
  size: 'md',
  seed: ''
});

const styleMap: Record<string, Style<any>> = {
  adventurer,
  avataaars,
  bigSmile,
  bottts,
  croodles,
  funEmoji,
  identicon,
  lorelei,
  micah,
  miniavs,
  notionists,
  openPeeps,
  personas,
  pixelArt,
  thumbs
};

const defaultStyle = 'avataaars';

function parseSeed(raw: string): { style: string; seed: string } {
  const colonIndex = raw.indexOf(':');
  if (colonIndex > 0) {
    const styleKey = raw.substring(0, colonIndex);
    const actualSeed = raw.substring(colonIndex + 1);
    if (styleMap[styleKey]) {
      return { style: styleKey, seed: actualSeed };
    }
  }
  return { style: defaultStyle, seed: raw };
}

const sizeClass = computed(() => {
  const map: Record<string, string> = {
    xs: 'h-5 w-5',
    sm: 'h-6 w-6',
    md: 'h-8 w-8',
    lg: 'h-9 w-9',
    xl: 'h-10 w-10',
    '2xl': 'h-20 w-20'
  };
  return map[props.size] || map.md;
});

const dataUri = computed(() => {
  const raw = props.seed || props.name || 'default';
  const { style: styleKey, seed } = parseSeed(raw);
  const style = styleMap[styleKey] || styleMap[defaultStyle];
  const avatar = createAvatar(style, { seed });
  return avatar.toDataUri();
});
</script>
