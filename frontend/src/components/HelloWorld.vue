<template>
    <div class="hello-world">
        <h1>{{ msg }}</h1>
        <p>{{ description }}</p>
        <button @click="increment">Clicked {{ count }} times</button>
    </div>
</template>

<script setup>
import { ref, defineProps } from 'vue'

const props = defineProps({
    msg: {
        type: String,
        default: 'Hello World'
    }
})

const count = ref(0)
const description = 'A simple Vue component'

function increment() {
    count.value++
}
</script>

<style scoped>
.hello-world {
    font-family: Avenir, Helvetica, Arial, sans-serif;
    text-align: center;
    margin: 2rem;
}
button {
    margin-top: 1rem;
    padding: 0.5rem 1rem;
    border: none;
    background: #42b983;
    color: white;
    border-radius: 4px;
    cursor: pointer;
}
button:hover { opacity: 0.9; }
</style>