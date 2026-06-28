/// <reference types="vite/client" />

// Support importing .vue files in TypeScript
declare module '*.vue' {
	import type { DefineComponent } from 'vue'
	const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>
	export default component
}
