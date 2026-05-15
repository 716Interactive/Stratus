import http from '@/api/http';
import { FileObject } from '@/api/server/files/loadDirectory';

export default (templateId: string, directory: string): Promise<FileObject[]> => {
    return new Promise((resolve, reject) => {
        http.get(`/api/client/stratus/templates/${templateId}/files/list`, { params: { directory } })
            .then(({ data }) => resolve((data || []).map((file: any) => ({
                ...file,
                key: `file_${file.name}`,
                isEditable: () => file.isEditable,
                isArchiveType: () => false,
                modifiedAt: new Date(file.modifiedAt),
            }))))
            .catch(reject);
    });
};
