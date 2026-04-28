import request from '../utils/request'

interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface MovieItem {
  id: number
  doubanId: string
  title: string
  titleCn?: string
  titleEn?: string
  /** 豆瓣原始海报 URL（有防盗链，浏览器 <img> 可能 403） */
  posterUrl?: string
  /** 本地已下载的海报路径（形如 /static/posters/xxx.jpg），优先使用 */
  posterLocalPath?: string
  year?: number
  doubanRating?: number
  doubanVotes?: number
  localRating?: number
  localVotes?: number
  wishCount?: number
  collectCount?: number
  popularityScore?: number
  summary?: string
  summaryShort?: string
  durationText?: string
  genres: string[]
}

/**
 * 海报取值优先级：
 *   1. 本地已下载的（/static/posters/xxx.jpg，不受豆瓣防盗链影响）
 *   2. 豆瓣远程原图（做兜底，可能被 403）
 */
export function resolvePoster(movie: Partial<MovieItem> | null | undefined): string | undefined {
  if (!movie) return undefined
  return movie.posterLocalPath || movie.posterUrl || undefined
}

export interface PersonBrief {
  id: number
  name: string
  nameEn?: string
  avatarUrl?: string
  avatarLocalPath?: string
  profileUrl?: string
  roleName?: string
}

export interface MovieDetail extends MovieItem {
  releaseDate?: string
  imdbId?: string
  countries?: string[]
  languages?: string[]
  directors: PersonBrief[]
  actors: PersonBrief[]
  writers: PersonBrief[]
}

export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface MovieListParams {
  page?: number
  size?: number
  genre?: string
  year?: number
  ratingMin?: number
  keyword?: string
}

export async function listMovies(params: MovieListParams) {
  const res = await request.get('/movies', { params }) as ApiEnvelope<PageResponse<MovieItem>>
  return res.data
}

export async function searchMovies(keyword: string, page = 1, size = 12) {
  const res = await request.get('/movies/search', {
    params: { q: keyword, page, size },
  }) as ApiEnvelope<PageResponse<MovieItem>>
  return res.data
}

export async function getMovieDetail(id: number) {
  const res = await request.get(`/movies/${id}`) as ApiEnvelope<MovieDetail>
  return res.data
}

export async function listMovieGenres() {
  const res = await request.get('/movies/genres') as ApiEnvelope<string[]>
  return res.data
}
