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
                                    <div key={template.id} className={'bg-neutral-900 p-4 rounded shadow-lg border border-neutral-700'}>
                                        <h3 className={'font-bold text-lg'}>{template.name}</h3>
                                        <p className={'text-xs text-neutral-400 mb-4'}>
                                            ID: <code>{template.id}</code>
                                        </p>
                                        <Link 
                                            to={`/stratus/templates/${template.id}/files`} 
                                            className={'block text-center bg-cyan-600 hover:bg-cyan-500 text-white px-4 py-2 rounded text-sm transition-colors'}
                                        >
                                            Manage Files
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
