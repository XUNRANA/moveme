import type { ChartMovieItem, MovieItem } from '../api/movies'

export function chartItemToMovie(item: ChartMovieItem): MovieItem {
  return {
    id: item.movieId,
    doubanId: '',
    title: item.title || '',
    posterUrl: item.posterUrl,
    year: item.year,
    doubanRating: item.rating,
    genres: [],
  }
}
