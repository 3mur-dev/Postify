import {
  useRef,
  useState,
  type MouseEvent,
} from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

import type {
  InteractionSource,
  PostMedia,
} from "../../api/types";

import { trackInteraction } from "../../api/interactionApi";
import { toast } from "sonner";
import { resolveImageUrl } from "../../utils/image";

interface FeedMediaProps {
  media: PostMedia[];
  postPublicId: string;
  source: InteractionSource;
}

export default function FeedMedia({
  media,
  postPublicId,
  source,
}: FeedMediaProps) {
  const [currentIndex, setCurrentIndex] = useState(0);

  const videoRef = useRef<HTMLVideoElement | null>(null);

  /*
   * Prevent MEDIA_START from firing repeatedly
   * when the user pauses/resumes the same video.
   */
  const hasStarted = useRef(false);

  /*
   * Last position for MEDIA_PROGRESS throttling.
   */
  const lastProgressTime = useRef(0);

  /*
   * Used to detect seeking.
   */
  const lastKnownPosition = useRef(0);

  /*
   * Prevent MEDIA_COMPLETE from firing more than once.
   */
  const hasCompleted = useRef(false);

  if (!media || media.length === 0) {
    return null;
  }

  const current = media[currentIndex];

  function track(
    event: Parameters<typeof trackInteraction>[0]
  ) {
    void trackInteraction(event).catch(() => {
      toast.error("Failed to track interaction.");
    });
  }

  function resetVideoTracking() {
    hasStarted.current = false;
    lastProgressTime.current = 0;
    lastKnownPosition.current = 0;
    hasCompleted.current = false;
  }

  function previous(event: MouseEvent) {
    event.stopPropagation();

    videoRef.current?.pause();

    setCurrentIndex((index) =>
      index === 0 ? media.length - 1 : index - 1
    );

    resetVideoTracking();
  }

  function next(event: MouseEvent) {
    event.stopPropagation();

    videoRef.current?.pause();

    setCurrentIndex((index) =>
      index === media.length - 1 ? 0 : index + 1
    );

    resetVideoTracking();
  }

  return (
    <div className="relative overflow-hidden border-y border-slate-800 bg-black">

      {/* Media */}
      <div className="flex max-h-[650px] min-h-[300px] w-full items-center justify-center">

        {current.type === "VIDEO" ? (
          <video
            key={current.id}
            ref={videoRef}
            src={resolveImageUrl(current.url)}
            controls
            playsInline
            preload="metadata"
            className="max-h-[650px] w-full object-contain"

            /*
             * First playback.
             */
            onPlay={(event) => {
              const video = event.currentTarget;

              if (!hasStarted.current) {
                hasStarted.current = true;

                track({
                  postPublicId,
                  eventType: "MEDIA_START",
                  source,
                  positionMs: Math.round(
                    video.currentTime * 1000
                  ),
                });
              }
            }}

            /*
             * Fires periodically while playing.
             *
             * We throttle this to roughly every 5 seconds.
             */
            onTimeUpdate={(event) => {
              const video = event.currentTarget;

              if (
                !Number.isFinite(video.duration) ||
                video.duration <= 0
              ) {
                return;
              }

              const currentTime = video.currentTime;

              if (
                currentTime - lastProgressTime.current < 5
              ) {
                return;
              }

              lastProgressTime.current = currentTime;
              lastKnownPosition.current = currentTime;

              track({
                postPublicId,
                eventType: "MEDIA_PROGRESS",
                source,
                positionMs: Math.round(
                  currentTime * 1000
                ),
                durationMs: Math.round(
                  video.duration * 1000
                ),
                completionRatio:
                  currentTime / video.duration,
              });
            }}

            /*
             * User manually seeks.
             */
            onSeeking={(event) => {
              const video = event.currentTarget;

              const newPosition = video.currentTime;
              const oldPosition =
                lastKnownPosition.current;

              /*
               * Ignore tiny timing differences.
               */
              if (
                Math.abs(newPosition - oldPosition) < 2
              ) {
                return;
              }

              track({
                postPublicId,
                eventType: "MEDIA_SKIP",
                source,
                positionMs: Math.round(
                  newPosition * 1000
                ),
                durationMs: Number.isFinite(video.duration)
                  ? Math.round(video.duration * 1000)
                  : undefined,
                completionRatio:
                  Number.isFinite(video.duration) &&
                  video.duration > 0
                    ? newPosition / video.duration
                    : undefined,
              });

              lastKnownPosition.current = newPosition;
            }}

            /*
             * Video reached the end.
             */
            onEnded={(event) => {
              if (hasCompleted.current) {
                return;
              }

              hasCompleted.current = true;

              const video = event.currentTarget;

              track({
                postPublicId,
                eventType: "MEDIA_COMPLETE",
                source,
                positionMs: Math.round(
                  video.duration * 1000
                ),
                durationMs: Math.round(
                  video.duration * 1000
                ),
                completionRatio: 1,
              });
            }}
          />
        ) : (
          <img
            key={current.id}
            src={resolveImageUrl(current.url)}
            alt="Post media"
            loading="lazy"
            className="max-h-[650px] w-full object-contain"
          />
        )}

      </div>

      {/* Previous */}
      {media.length > 1 && (
        <button
          type="button"
          onClick={previous}
          data-stop-navigation="true"
          aria-label="Previous media"
          className="
            absolute left-3 top-1/2
            flex h-9 w-9 -translate-y-1/2
            -translate-y-1/2 items-center justify-center
            rounded-full
            bg-black/60
            text-white
            backdrop-blur-sm
            transition
            hover:bg-black/80
          "
        >
          <ChevronLeft className="h-5 w-5" />
        </button>
      )}

      {/* Next */}
      {media.length > 1 && (
        <button
          type="button"
          onClick={next}
          data-stop-navigation="true"
          aria-label="Next media"
          className="
            absolute right-3 top-1/2
            flex h-9 w-9 -translate-y-1/2
            items-center justify-center
            rounded-full
            bg-black/60
            text-white
            backdrop-blur-sm
            transition
            hover:bg-black/80
          "
        >
          <ChevronRight className="h-5 w-5" />
        </button>
      )}

      {/* Position indicator */}
      {media.length > 1 && (
        <div className="absolute right-3 top-3 rounded-full bg-black/60 px-2.5 py-1 text-xs font-medium text-white backdrop-blur-sm">
          {currentIndex + 1}/{media.length}
        </div>
      )}

      {/* Dots */}
      {media.length > 1 && (
        <div className="absolute bottom-3 left-1/2 flex -translate-x-1/2 gap-1.5">
          {media.map((item, index) => (
            <button
              key={item.id}
              type="button"
              data-stop-navigation="true"
              onClick={(event) => {
                event.stopPropagation();

                videoRef.current?.pause();

                setCurrentIndex(index);
                resetVideoTracking();
              }}
              aria-label={`Go to media ${index + 1}`}
              className={`
                h-1.5 rounded-full transition-all
                ${
                  index === currentIndex
                    ? "w-4 bg-white"
                    : "w-1.5 bg-white/50 hover:bg-white/80"
                }
              `}
            />
          ))}
        </div>
      )}
    </div>
  );
}