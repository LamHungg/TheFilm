export type AgeRating = 'P' | 'K' | 'T13' | 'T16' | 'T18' | 'C';

export interface Movie {
  id: string;
  title: string;
  originalTitle?: string;
  slug: string;
  posterUrl: string;
  bannerUrl: string;
  rating: AgeRating;
  ratingDesc: string;
  duration: number; // in minutes
  releaseDate: string;
  status: 'NOW_SHOWING' | 'COMING_SOON';
  genres: string[];
  formats: ('2D' | '3D' | 'IMAX' | '4DX')[];
  languages: string[];
  director: string;
  cast: string[];
  synopsis: string;
  trailerUrl: string;
  score: number; // e.g. 9.2
  figmaTheme: 'navy' | 'coral' | 'purple' | 'teal';
  category?: string;
}
