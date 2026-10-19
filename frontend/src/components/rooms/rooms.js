// Room types and equipment: names and icons for the rooms page.
export const ROOM_TYPES = {
  CLASSROOM: { label: 'Sinfxona', icon: 'class' },
  LAB: { label: 'Laboratoriya', icon: 'science' },
  COMPUTER_LAB: { label: 'Kompyuter xonasi', icon: 'computer' },
  GYM: { label: 'Sport zali', icon: 'sports_basketball' },
  ASSEMBLY_HALL: { label: 'Faollar zali', icon: 'theater_comedy' },
  LIBRARY: { label: 'Kutubxona', icon: 'local_library' },
  OTHER: { label: 'Boshqa', icon: 'meeting_room' }
}

export const EQUIPMENT = {
  PROJECTOR: { label: 'Proyektor', icon: 'videocam' },
  SMARTBOARD: { label: 'Interaktiv doska', icon: 'touch_app' },
  AC: { label: 'Konditsioner', icon: 'ac_unit' },
  COMPUTERS: { label: 'Kompyuterlar', icon: 'computer' }
}

export const STATES = {
  BUSY: { label: 'Band', color: 'var(--room-busy)' },
  BOOKED: { label: 'Band qilingan', color: 'var(--room-booked)' },
  FREE: { label: "Bo'sh", color: 'var(--room-free)' },
  REPAIR: { label: "Ta'mirda", color: 'var(--room-repair)' }
}

export const hm = t => String(t || '').slice(0, 5)
