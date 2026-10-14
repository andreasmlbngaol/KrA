import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

/** Renders agent Markdown (including GFM tables) into HTML. */
export function Markdown({ content }: { content: string }) {
  return (
    <div className="markdown text-[15px] leading-relaxed text-neutral-800">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
    </div>
  );
}
