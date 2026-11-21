import type { InteractionSource } from "../../api/types";
import { trackInteraction } from "../../api/interactionApi";

interface PostContentProps {
  content: string;
  postPublicId: string;
  source: InteractionSource;
}

const URL_REGEX = /(https?:\/\/[^\s<]+)/gi;

export default function PostContent({
  content,
  postPublicId,
  source,
}: PostContentProps) {
  const parts = content.split(URL_REGEX);

  return (
    <p className="whitespace-pre-wrap break-words text-sm leading-6 text-slate-200">
      {parts.map((part, index) => {
        const isUrl = /^https?:\/\//i.test(part);

        if (!isUrl) {
          return <span key={index}>{part}</span>;
        }

        return (
          <a
            key={index}
            href={part}
            target="_blank"
            rel="noopener noreferrer"
            data-stop-navigation="true"
            className="text-blue-400 underline transition hover:text-blue-300"
            onClick={() => {
              void trackInteraction({
                postPublicId,
                eventType: "LINK_CLICK",
                source,
                metadata: part,
              }).catch((error) => {
                console.error("Failed to track link click:", error);
              });
            }}
          >
            {part}
          </a>
        );
      })}
    </p>
  );
}