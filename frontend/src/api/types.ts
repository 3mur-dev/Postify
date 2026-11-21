export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export type InteractionSource =
  |  "FEED"
  |  "PROFILE"
  |  "SEARCH"
  |  "SAVED_POSTS"
  |  "NOTIFICATIONS"
  |  "DIRECT"
  |  "SHARED"
  |  "HASHTAG"
  |  "RECOMMENDED"
  |  "POST_DETAIL"
  |  "OTHER"

 export type InteractionEventType =
  | "IMPRESSION"
  | "VIEW"
  | "VIEW_START"
  | "VIEW_END"
  | "LIKE"
  | "UNLIKE"
  | "SAVE"
  | "UNSAVE"
  | "COMMENT"
  | "SHARE"
  | "PROFILE_OPEN"
  | "POST_OPEN"
  | "MEDIA_START"
  | "MEDIA_PROGRESS"
  | "MEDIA_COMPLETE"
  | "MEDIA_SKIP"
  | "FOLLOW"
  | "UNFOLLOW"
  | "SEARCH_CLICK"
  | "LINK_CLICK"
  | "HIDE"
  | "REPORT";
export type MediaType = "IMAGE" | "VIDEO";

export interface TrackInteractionRequest {
  postPublicId: string;
  eventType: InteractionEventType;
  source: InteractionSource;

  sessionId?: string;
  clientEventId?: string;

  positionMs?: number;
  durationMs?: number;
  completionRatio?: number;

  metadata?: string;
}

export interface PostMedia {
  id: number;
  url: string;
  type: MediaType;
  position: number;
  width: number | null;
  height: number | null;
  durationMs: number | null;
}

export interface PostFeedDto {
  id: number;
  publicId: string;
  content: string | null;
  media: PostMedia[];
  createdAt: string;
  username: string;
  avatarUrl: string | null;
  likeCount: number;
  commentCount: number;
  likedByCurrentUser: boolean;
  savedByCurrentUser: boolean;
}

export type ProfileResponse = {
  id: number;
  publicId: string;
  username: string;
  bio: string;
  avatarUrl: string;
  ownProfile: boolean;
  following: boolean;
  followersCount: number;
  followingCount: number;
  posts: ProfilePostDto[];
  role: Role;
};

export interface ProfilePostDto {
  id: number;
  publicId: string;
  content: string | null;
  media: PostMedia[];
  createdAt: string;
  likeCount: number;
  commentCount: number;
  username: string;
}

export interface SavedPostResponse {
  savedAt: string;
  post: PostFeedDto;
}

export interface AuthLoginPayload {
  username: string;
  password: string;
}

export interface AuthRegisterPayload {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface AuthTokenResponse {
  token: string;
}

export interface CommentDto {
  id: number;
  content: string;
  createdAt: string;
  userPublicId: string;
  username: string;
  userAvatarUrl: string | null;
  postPublicId: string;
}

export type UserSortField =
  |  "CREATED_AT"
  |  "USERNAME"
  |  "EMAIL"
  |  "STATUS";

export type UserStatus =
  | "ACTIVE"
  | "SUSPENDED"
  | "BANNED";

export type PostStatus = 
  | "ACTIVE"
  | "HIDDEN"
  | "DELETED"
  | "UNDER_REVIEW"
  | "REPORTED";

export type ReportReason =
  | "SPAM"
  | "HARASSMENT"
  | "HATE_SPEECH"
  | "MISINFORMATION"
  | "OTHER";

export type RangePreset =
  | "TODAY"
  | "LAST_7_DAYS"
  | "LAST_30_DAYS"
  | "LAST_90_DAYS"
  | "CUSTOM";

export type DashboardSection =
  | "OVERVIEW"
  | "CHARTS"
  | "SYSTEM_METRICS"
  | "USER_METRICS"
  | "ENGAGEMENT_METRICS";

export type ReportStatus =
  | "PENDING"
  | "RESOLVED"
  | "REJECTED";

export type Role =
  |  "USER"
  |  "ADMIN";


export interface AdminUser {
  publicId: string;
  username: string;
  email: string;
  status: UserStatus;
  createdAt: string;
}

export interface AdminUserDetails extends AdminUser {

  bio: string | null;
  avatarUrl: string | null;
  followersCount: number;
  followingCount: number;
  postsCount: number;
  role: Role;
}

export interface AdminPost {
  publicId: string;
  userPublicId: string;
  content: string | null;
  media: PostMedia[];
  status: PostStatus;
  username: string;
  likeCount: number;
  commentCount: number;
  reportCount: number;
  lastReportReason: ReportReason | null;
  createdAt: string;
  updatedAt: string;
  deleted: boolean;
}

export interface AdminReport {
  publicId: string;
  postPublicId: string;
  reporterUsername: string;
  reason: ReportReason;
  status: ReportStatus;
  createdAt: string;
}

export interface AdminDashboardResponse {
  overview: OverviewMetrics;
  users: UserMetrics;
  engagement: EngagementMetrics;
  system: SystemMetrics;
  userGrowthChart: ChartDataPoint[];
  postGrowthChart: ChartDataPoint[];
  engagementChart: ChartDataPoint[];
}

export interface AdminDashboardRequest {
  rangePreset: RangePreset;
  fromDate?: string;
  toDate?: string;
  timezone: string;
  sections: DashboardSection[];
}

export interface ChartDataPoint {
  label: string;
  value: number;
}

export interface EngagementMetrics {

        likesReceived: number ,

        commentsReceived: number ,

        averageLikesPerPost: number,

        averageCommentsPerPost: number,

        engagementRate: number
}

export interface OverviewMetrics{

        totalUsers: number,
        activeUsers: number,

        totalPosts: number,
        totalComments: number,
        totalLikes: number
}

export interface SystemMetrics{

        cpuUsage: number,
        memoryUsage: number,
        diskUsage: number,
        uptimeSeconds: number
}

export interface UserMetrics{
        bannedUsers: number,
        suspendedUsers: number,
        newUsersInPeriod: number,
        userGrowthPercentage: number
}

export interface NotificationResponse {
    publicId: string;
    message: string;
    type: "LIKE" | "COMMENT" | "FOLLOW" | "SYSTEM";
    actorPublicId: string | null;
    actorUsername: string | null;
    postPublicId: string | null;
    commentId: number | null;
    isRead: boolean;
    createdAt: string;
}

export interface DeletePostVariables {
  publicId: string;
  profileQueryUsername: string;
}

export interface EditPostVariables {
  publicId: string;
  content: string;
  profileQueryUsername: string;
}

export interface UserSearchResult {
  id: number;
  publicId: string;
  username: string;
  displayName: string;
  avatarUrl: string | null;
}

export interface SearchResponse {
  users: UserSearchResult[];
  posts: PostFeedDto[];
}

export interface ConversationParticipant {
  publicId: string;
  username: string;
  avatarUrl: string | null;
}

export interface ConversationMessagePreview {
  content: string;
  senderUsername: string;
  createdAt: string;
}

export interface ConversationSummary {
  publicId: string;
  participant: ConversationParticipant;
  latestMessage: ConversationMessagePreview | null;
  updatedAt: string;
}

export interface ConversationCreatedResponse {
  publicId: string;
}

export interface DirectMessage {
  id: number;
  conversationPublicId: string;
  senderPublicId: string;
  senderUsername: string;
  senderAvatarUrl: string | null;
  content: string;
  clientMessageId: string | null;
  createdAt: string;
  read: boolean;
  readAt: string | null;
}

export interface SpringPage<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  last: boolean;
  first: boolean;
  empty: boolean;
}