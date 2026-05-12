import React from 'react';
import PageContentBlock from '@/components/elements/PageContentBlock';
import { useStoreState } from 'easy-peasy';
import { ApplicationStore } from '@/state';

export default () => {
    const user = useStoreState((state: ApplicationStore) => state.user.data!);

    return (
        <PageContentBlock title={'Server Groups'} showFlashKey={'stratus:groups'}>
            <div className={'flex flex-wrap'}>
                <div className={'w-full'}>
                    <div className={'bg-neutral-800 p-6 rounded shadow-md'}>
                        <h2 className={'text-2xl font-header font-medium'}>Stratus Server Groups</h2>
                        <p className={'text-neutral-400'}>Manage your automated network clusters here.</p>
                        <hr className={'my-4 border-neutral-700'} />
                        <p className={'text-neutral-500'}>You currently have access to manage groups associated with your account.</p>
                        {/* Group list and management logic will go here */}
                    </div>
                </div>
            </div>
        </PageContentBlock>
    );
};
