import type { ChatMessageItem } from '../types/ui';
import { Markdown } from './Markdown';

export function ChatMessage({ message }: { message: ChatMessageItem }) {
  const isUser = message.role === 'user';
  const bubble = isUser
    ? 'bg-usu text-white'
    : message.isError
      ? 'border border-red-200 bg-red-50 text-red-700'
      : 'border border-neutral-200 bg-white text-neutral-800';

  return (
    <div className={`flex flex-col ${isUser ? 'items-end' : 'items-start'}`}>
      <div className={`mb-1 text-xs font-medium ${isUser ? 'text-usu' : 'text-neutral-500'}`}>
        {isUser ? 'Kamu' : 'KRS Agent'}
      </div>
      <div className={`max-w-[85%] rounded-2xl px-4 py-3 shadow-sm ${bubble}`}>
        {isUser ? (
          <p className="whitespace-pre-wrap">{message.content}</p>
        ) : (
          <Markdown content={message.content} />
        )}
      </div>
    </div>
  );
}
