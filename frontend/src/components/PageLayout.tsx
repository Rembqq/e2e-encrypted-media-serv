interface Props {
    children: React.ReactNode
    title: string
    backTo?: string
    backLabel?: string
  }
  
  export default function PageLayout({ children, title, backTo, backLabel }: Props) {
    return (
      <div className="min-h-screen bg-gray-50">
        <div className="max-w-4xl mx-auto px-4 py-6">
          <div className="flex items-center justify-between mb-8">
            <h1 className="text-2xl font-bold">{title}</h1>
            {backTo && (
              <a href={backTo} className="text-sm text-gray-500 hover:text-gray-700 underline">
                &larr; {backLabel ?? 'Back'}
              </a>
            )}
          </div>
          {children}
        </div>
      </div>
    )
  }