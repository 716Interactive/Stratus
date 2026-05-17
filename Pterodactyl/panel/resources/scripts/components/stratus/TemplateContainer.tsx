import React from 'react';
import PageContentBlock from '@/components/elements/PageContentBlock';
import useSWR from 'swr';
import http from '@/api/http';
import Spinner from '@/components/elements/Spinner';
import { Link } from 'react-router-dom';

export default () => {
    const { data: templates, error } = useSWR('/api/client/stratus/templates', (url) => 
        http.get(url).then(res => res.data)
    );

    if (error) return <PageContentBlock title={'Error'}><div>Failed to load templates.</div></PageContentBlock>;

    return (
        <PageContentBlock title={'Server Templates'} showFlashKey={'stratus:templates'}>
            <div className={'flex flex-wrap'}>
                <div className={'w-full'}>
                    <div className={'bg-neutral-800 p-6 rounded shadow-md'}>
                        <h2 className={'text-2xl font-header font-medium'}>Stratus Templates</h2>
                        <p className={'text-neutral-400'}>Manage your "Golden Images" and network blueprints.</p>
                        <hr className={'my-4 border-neutral-700'} />
                        
                        {!templates ? <Spinner centered /> : (
                            <div className={'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4'}>
                                {templates.map((template: any) => (
                                    <div key={template.id} className={'bg-neutral-900 p-5 rounded-lg shadow-lg border border-neutral-700 hover:border-cyan-500/50 transition-all duration-200 group flex flex-col justify-between'}>
                                        <div>
                                            <h3 className={'font-bold text-lg text-neutral-100 group-hover:text-cyan-400 transition-colors'}>{template.name}</h3>
                                            <p className={'text-xs text-neutral-500 font-mono mt-1 mb-4'}>
                                                ID: {template.id}
                                            </p>
                                            <div className={'text-xs text-neutral-400 mb-4 space-y-1'}>
                                                <div>Path: <code className={'text-neutral-300 font-mono text-[10px]'}>{template.localPath}</code></div>
                                            </div>
                                        </div>
                                        <Link 
                                            to={`/stratus/templates/${template.id}`} 
                                            className={'block text-center bg-cyan-600 hover:bg-cyan-500 text-white px-4 py-2 rounded text-sm font-medium transition-colors'}
                                        >
                                            Open Template
                                        </Link>
                                    </div>
                                ))}
                                {templates.length === 0 && (
                                    <div className={'col-span-full p-10 text-center text-neutral-500'}>
                                        You don&apos;t have any templates yet. Create one in the Admin panel!
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </PageContentBlock>
    );
};
