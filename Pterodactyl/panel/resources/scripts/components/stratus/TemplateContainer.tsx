import React from 'react';
import PageContentBlock from '@/components/elements/PageContentBlock';

export default () => {
    return (
        <PageContentBlock title={'Server Templates'} showFlashKey={'stratus:templates'}>
            <div className={'flex flex-wrap'}>
                <div className={'w-full'}>
                    <div className={'bg-neutral-800 p-6 rounded shadow-md'}>
                        <h2 className={'text-2xl font-header font-medium'}>Stratus Templates</h2>
                        <p className={'text-neutral-400'}>Manage your "Golden Images" and network blueprints.</p>
                        <hr className={'my-4 border-neutral-700'} />
                        <div className={'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4'}>
                            {/* Template cards with File Manager buttons */}
                            <div className={'bg-neutral-700 p-4 rounded'}>
                                <h3 className={'font-bold'}>Example Template</h3>
                                <p className={'text-xs text-neutral-400 mb-4'}>Last modified: 2 hours ago</p>
                                <a 
                                    href={'/stratus/templates/example-id/files'} 
                                    className={'bg-cyan-600 hover:bg-cyan-500 text-white px-4 py-2 rounded text-sm transition-colors'}
                                >
                                    Edit Template Files
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </PageContentBlock>
    );
};
