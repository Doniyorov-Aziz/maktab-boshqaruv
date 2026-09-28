import { defineStore } from 'pinia'

function readStoredId() {
  try {
    const saved = localStorage.getItem('activeSchoolId')
    return saved ? Number(saved) : null
  } catch {
    return null
  }
}

function readStoredName() {
  try {
    return localStorage.getItem('activeSchoolName') || null
  } catch {
    return null
  }
}

export const useSchoolStore = defineStore('school', {
  state: () => ({
    activeSchoolId: readStoredId(),
    activeSchoolName: readStoredName(),
    schools: [],
    loaded: false
  }),

  actions: {
    setActiveSchool(id, name) {
      this.activeSchoolId = id
      this.activeSchoolName = name
      try {
        localStorage.setItem('activeSchoolId', String(id))
        if (name) localStorage.setItem('activeSchoolName', name)
      } catch {
        // localStorage unavailable — selection still works for this session
      }
    },

    clearActiveSchool() {
      this.activeSchoolId = null
      this.activeSchoolName = null
      try {
        localStorage.removeItem('activeSchoolId')
        localStorage.removeItem('activeSchoolName')
      } catch {
        // ignore
      }
    },

    async fetchSchools(api) {
      const response = await api.get('/api/schools', { params: { size: 1000 } })
      this.schools = response.data.content
      this.loaded = true

      const stillExists = this.schools.some(s => s.id === this.activeSchoolId)
      if (!stillExists) {
        if (this.schools.length > 0) {
          this.setActiveSchool(this.schools[0].id, this.schools[0].name)
        } else {
          this.clearActiveSchool()
        }
      } else {
        const current = this.schools.find(s => s.id === this.activeSchoolId)
        if (current && current.name !== this.activeSchoolName) {
          this.setActiveSchool(current.id, current.name)
        }
      }

      return this.schools
    }
  }
})
