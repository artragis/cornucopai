import { createApp } from 'vue'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
// Material Design Icons font — Vuetify's default icon set (mdi) renders the checkbox
// visuals from these glyphs; without the font the checkboxes are invisible (review bug).
import '@mdi/font/css/materialdesignicons.min.css'
import App from './App.vue'
import { router } from './router'

const vuetify = createVuetify({ components, directives })

// T025 (US4): install the router for the dedicated /ateliers/:id route
createApp(App).use(vuetify).use(router).mount('#app')
