// Mirrors backend ValidationLimits.java. The backend is the source of truth;
// these exist so the form can give feedback before submitting.
export const LIMITS = {
  customerNameMax: 80,
  subjectMin: 5,
  subjectMax: 120,
  descriptionMin: 10,
  descriptionMax: 2000,
  messageMax: 1000,
  explanationMin: 5,
  explanationMax: 500,
} as const;
