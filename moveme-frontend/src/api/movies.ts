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

export interface ReleaseDateVO {
  date?: string
  region?: string
  rawText?: string
}

export interface AwardVO {
  ceremony?: string
  category?: string
  status?: string
  recipient?: string
  url?: string
}

export interface CommentVO {
  id?: number
  authorName?: string
  authorAvatar?: string
  authorLocation?: string
  rating?: number
  ratingLabel?: string
  content?: string
  votes?: number
  postedAt?: string
  sourceUrl?: string
  liked?: boolean
}

export interface RelatedMovieVO {
  movieId?: number
  title?: string
  rating?: number
  coverUrl?: string
}

export interface RatingDistVO {
  star?: number
  label?: string
  percentage?: number
}

export interface GenreRankVO {
  genre?: string
  percentile?: number
}

export interface GenreCard {
  name: string
  posterUrl: string
  movieCount: number
  size: 'large' | 'medium' | 'small'
}

export interface Top250VO {
  rank?: number
  listTitle?: string
  quote?: string
}

export interface MovieDetail extends MovieItem {
  releaseDate?: string
  imdbId?: string
  countries?: string[]
  languages?: string[]
  directors: PersonBrief[]
  actors: PersonBrief[]
  writers: PersonBrief[]
  akas?: string[]
  tags?: string[]
  releaseDates?: ReleaseDateVO[]
  awards?: AwardVO[]
  relatedMovies?: RelatedMovieVO[]
  ratingDist?: RatingDistVO[]
  genreRanks?: GenreRankVO[]
  top250?: Top250VO
  playLinks?: PlayLinkVO[]
}

export interface PlayLinkVO {
  platform?: string
  url?: string
}

export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
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

export async function getMovieComments(movieId: number, page = 1, size = 10, sort = 'hot') {
  const res = await request.get(`/movies/${movieId}/comments`, { params: { page, size, sort } }) as ApiEnvelope<PageResponse<CommentVO>>
  return res.data
}

export async function submitMovieComment(movieId: number, content: string, rating?: number) {
  const res = await request.post(`/movies/${movieId}/comments`, { content, rating }) as ApiEnvelope<void>
  return res.data
}

export async function likeComment(commentId: number) {
  const res = await request.post(`/movies/comments/${commentId}/like`) as ApiEnvelope<void>
  return res.data
}

export async function unlikeComment(commentId: number) {
  const res = await request.delete(`/movies/comments/${commentId}/like`) as ApiEnvelope<void>
  return res.data
}

export interface FilmographyItem {
  movieId: number
  title: string
  posterUrl?: string
  rating?: number
  year?: number
  roleName?: string
}

export interface PersonDetail {
  id: number
  name: string
  nameEn?: string
  avatarUrl?: string
  avatarLocalPath?: string
  gender?: string
  birthDate?: string
  birthPlace?: string
  bio?: string
  movieCount?: number
  avgMovieRating?: number
  directed: FilmographyItem[]
  written: FilmographyItem[]
  acted: FilmographyItem[]
}

export async function getPersonDetail(id: number) {
  const res = await request.get(`/persons/${id}`) as ApiEnvelope<PersonDetail>
  return res.data
}

export interface ChartMovieItem {
  movieId: number
  title: string
  posterUrl?: string
  rating?: number
  year?: number
  rankNo?: number
}

export interface MovieChart {
  genreName: string
  boardTitle: string
  movies: ChartMovieItem[]
}

export async function listChartGenres() {
  const res = await request.get('/movies/chart-genres') as ApiEnvelope<string[]>
  return res.data
}

export async function getChartByGenre(genre: string) {
  const res = await request.get(`/movies/chart-genres/${encodeURIComponent(genre)}`) as ApiEnvelope<MovieChart>
  return res.data
}

export async function listAnnualYears() {
  const res = await request.get('/movies/annual-years') as ApiEnvelope<number[]>
  return res.data
}

export async function getAnnualByYear(year: number) {
  const res = await request.get(`/movies/annual/${year}`) as ApiEnvelope<MovieChart[]>
  return res.data
}

export interface BoardVO {
  boardName: string
  displayName: string
  genreName: string
  boardTitle: string
}

export async function listBoards() {
  const res = await request.get('/movies/boards') as ApiEnvelope<BoardVO[]>
  return res.data
}

export async function getBoardMovies(boardName: string) {
  const res = await request.get(`/movies/boards/${encodeURIComponent(boardName)}`) as ApiEnvelope<MovieChart>
  return res.data
}

export async function getTop250() {
  const res = await request.get('/movies/top250') as ApiEnvelope<MovieChart>
  return res.data
}

export interface DiscoverSection {
  key: string
  title: string
  movies: MovieItem[]
}

export async function getDiscover() {
  const res = await request.get('/movies/discover') as ApiEnvelope<DiscoverSection[]>
  return res.data
}

// ─── User APIs ───

export interface UserStats {
  ratingCount: number
  wishCount: number
  watchedCount: number
  historyCount: number
}

export interface UserRatingItem {
  id: number
  movieId: number
  title: string
  posterUrl?: string
  posterLocalPath?: string
  score: number
  comment?: string
  createdAt: string
}

export interface UserFavoriteItem {
  id: number
  movieId: number
  title: string
  posterUrl?: string
  posterLocalPath?: string
  doubanRating?: number
  year?: number
  genres: string[]
  status: number
  createdAt: string
}

export interface UserHistoryItem {
  id: number
  movieId: number
  title: string
  posterUrl?: string
  posterLocalPath?: string
  viewedAt: string
}

export interface GenrePreference {
  genreId: number
  genreName: string
  score: number
}

export interface PersonPreference {
  personId: number
  personName: string
  roleKind: string
  score: number
}

export interface UserTaste {
  genrePrefs: GenrePreference[]
  personPrefs: PersonPreference[]
  avgRatingGiven: number
  ratingCount: number
}

export async function getUserStats() {
  const res = await request.get('/users/me/stats') as ApiEnvelope<UserStats>
  return res.data
}

export async function getUserRatings(page = 1, size = 10) {
  const res = await request.get('/users/me/ratings', { params: { page, size } }) as ApiEnvelope<PageResponse<UserRatingItem>>
  return res.data
}

export async function getUserFavorites(status?: number, page = 1, size = 12) {
  const res = await request.get('/users/me/favorites', { params: { status, page, size } }) as ApiEnvelope<PageResponse<UserFavoriteItem>>
  return res.data
}

export async function getUserHistory(page = 1, size = 20) {
  const res = await request.get('/users/me/history', { params: { page, size } }) as ApiEnvelope<PageResponse<UserHistoryItem>>
  return res.data
}

export async function getUserTaste() {
  const res = await request.get('/users/me/taste') as ApiEnvelope<UserTaste>
  return res.data
}

export async function uploadAvatar(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await request.post('/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }) as ApiEnvelope<string>
  return res.data
}

// ─── Favorite / Rating / History APIs ───

export interface FavoriteStatus {
  id: number
  movieId: number
  status: number
  createdAt: string
}

export interface RatingStatus {
  id: number
  movieId: number
  score: number
  comment: string
  createdAt: string
}

export async function checkFavorite(movieId: number): Promise<FavoriteStatus | null> {
  const res = await request.get('/users/me/favorites/check', { params: { movieId } }) as ApiEnvelope<FavoriteStatus | null>
  return res.data
}

export async function addFavorite(movieId: number, status: number): Promise<void> {
  await request.post('/users/me/favorites', { movieId, status })
}

export async function removeFavorite(movieId: number): Promise<void> {
  await request.delete('/users/me/favorites', { params: { movieId } })
}

export async function checkRating(movieId: number): Promise<RatingStatus | null> {
  const res = await request.get('/users/me/ratings/check', { params: { movieId } }) as ApiEnvelope<RatingStatus | null>
  return res.data
}

export async function saveRating(movieId: number, score: number, comment?: string): Promise<void> {
  await request.post('/users/me/ratings', { movieId, score, comment })
}

export async function recordView(movieId: number): Promise<void> {
  await request.post('/users/me/history', { movieId })
}

// ─── Admin APIs ───

export interface AdminStats {
  movieCount: number
  userCount: number
  todayNewUsers: number
  ratingCount: number
  lastCrawlStatus?: string
  lastCrawlTime?: string
  recoLogCount: number
  favoriteCount: number
  viewHistoryCount: number
  searchHistoryCount: number
  personCount: number
  genreCount: number
  crawlLogCount: number
  importLogCount: number
  movieCommentCount: number
}

export interface AdminUserItem {
  id: number
  username: string
  nickname?: string
  email?: string
  role: number
  status: number
  createdAt: string
}

export interface CrawlLogItem {
  id: number
  taskType: string
  status: string
  totalCount: number
  successCount: number
  failCount: number
  errorMessage?: string
  startedAt: string
  finishedAt?: string
}

export interface ImportLogItem {
  id: number
  source: string
  filePath: string
  moviesTotal: number
  moviesOk: number
  moviesFail: number
  personsOk: number
  commentsOk: number
  errors?: string
  startedAt: string
  finishedAt?: string
}

export interface RecoLogItem {
  id: number
  userId: number
  strategyType: string
  llmProvider?: string
  latencyMs?: number
  createdAt: string
}

export async function getAdminStats() {
  const res = await request.get('/admin/stats') as ApiEnvelope<AdminStats>
  return res.data
}

export async function getAdminUsers(params: { page?: number; size?: number; keyword?: string; role?: number; status?: number }) {
  const res = await request.get('/admin/users', { params }) as ApiEnvelope<PageResponse<AdminUserItem>>
  return res.data
}

export async function updateUserStatus(userId: number, status: number) {
  const res = await request.put(`/admin/users/${userId}/status`, { status }) as ApiEnvelope<void>
  return res.data
}

export async function updateUserRole(userId: number, role: number) {
  const res = await request.put(`/admin/users/${userId}/role`, { role }) as ApiEnvelope<void>
  return res.data
}

export async function getAdminCrawlLogs(page = 1, size = 20) {
  const res = await request.get('/admin/crawl-logs', { params: { page, size } }) as ApiEnvelope<PageResponse<CrawlLogItem>>
  return res.data
}

export async function getAdminImportLogs(page = 1, size = 20) {
  const res = await request.get('/admin/import-logs', { params: { page, size } }) as ApiEnvelope<PageResponse<ImportLogItem>>
  return res.data
}

export async function getAdminRecoLogs(page = 1, size = 20) {
  const res = await request.get('/admin/reco-logs', { params: { page, size } }) as ApiEnvelope<PageResponse<RecoLogItem>>
  return res.data
}

// ============ AI 推荐 ============

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
}

export async function quickRecommend() {
  const res = await request.post('/recommend/quick') as ApiEnvelope<string>
  return res.data
}

/**
 * 聊天推荐 — 非流式（通过 axios，稳定可靠）
 */
export async function chatRecommend(messages: ChatMessage[]): Promise<string> {
  const res = await request.post('/recommend/chat', { messages }) as ApiEnvelope<string>
  return res.data
}
