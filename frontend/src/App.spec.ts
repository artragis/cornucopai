import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { router } from './router'
import App from './App.vue'

describe('App', () => {
  it('renders the router-view shell without crashing', async () => {
    await router.push('/')
    await router.isReady()
    const wrapper = mount(App, { global: { plugins: [router] } })
    expect(wrapper.exists()).toBe(true)
  })
})
