/*
 * Copyright (C) 2024 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

import { useQueryClient } from '@tanstack/react-query';
import { createFileRoute, Link } from '@tanstack/react-router';
import {
  CellContext,
  ColumnDef,
  getCoreRowModel,
  useReactTable,
} from '@tanstack/react-table';
import { EditIcon, PlusIcon } from 'lucide-react';

import {
  useProductsServiceDeleteApiV1ProductsByProductIdSecretsBySecretName,
  useProductsServiceGetApiV1ProductsByProductId,
  useProductsServiceGetApiV1ProductsByProductIdSecrets,
  useProductsServiceGetApiV1ProductsByProductIdSecretsKey,
} from '@/api/queries';
import {
  prefetchUseProductsServiceGetApiV1ProductsByProductId,
  prefetchUseProductsServiceGetApiV1ProductsByProductIdSecrets,
} from '@/api/queries/prefetch';
import { useProductsServiceGetApiV1ProductsByProductIdSuspense } from '@/api/queries/suspense';
import { ApiError, Secret } from '@/api/requests';
import { DataTable } from '@/components/data-table/data-table';
import { DeleteDialog } from '@/components/delete-dialog';
import { DeleteIconButton } from '@/components/delete-icon-button';
import { LoadingIndicator } from '@/components/loading-indicator';
import { ToastError } from '@/components/toast-error';
import { Button } from '@/components/ui/button';
import { buttonVariants } from '@/components/ui/button-variants';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { toast } from '@/lib/toast';
import { cn } from '@/lib/utils';
import { paginationSearchParameterSchema } from '@/schemas';

const defaultPageSize = 10;

const ActionCell = ({ row }: CellContext<Secret, unknown>) => {
  const params = Route.useParams();
  const queryClient = useQueryClient();

  const { data: product } =
    useProductsServiceGetApiV1ProductsByProductIdSuspense({
      productId: Number.parseInt(params.productId),
    });

  const { mutateAsync: deleteSecret } =
    useProductsServiceDeleteApiV1ProductsByProductIdSecretsBySecretName({
      onSuccess() {
        toast.info('Delete Secret', {
          description: `Secret "${row.original.name}" deleted successfully.`,
        });
        queryClient.invalidateQueries({
          queryKey: [useProductsServiceGetApiV1ProductsByProductIdSecretsKey],
        });
      },
      onError(error: ApiError) {
        toast.error(error.message, {
          description: <ToastError error={error} />,
          duration: Infinity,
          cancel: {
            label: 'Dismiss',
            onClick: () => {},
          },
        });
      },
    });

  return (
    <div className='flex items-center justify-end gap-1'>
      <Tooltip>
        <TooltipTrigger asChild>
          <Link
            to='/organizations/$orgId/products/$productId/secrets/$secretName/edit'
            params={{
              orgId: params.orgId,
              productId: params.productId,
              secretName: row.original.name,
            }}
            className={cn(buttonVariants({ variant: 'outline' }), 'h-8 px-2')}
          >
            <span className='sr-only'>Edit</span>
            <EditIcon size={16} />
          </Link>
        </TooltipTrigger>
        <TooltipContent>Edit this secret</TooltipContent>
      </Tooltip>
      <DeleteDialog
        thingName={'secret'}
        uiComponent={<DeleteIconButton />}
        onDelete={async () =>
          await deleteSecret({
            productId: product.id,
            secretName: row.original.name,
          })
        }
      />
    </div>
  );
};

const columns: ColumnDef<Secret>[] = [
  {
    accessorKey: 'name',
    header: 'Name',
    enableColumnFilter: false,
  },
  {
    accessorKey: 'description',
    header: 'Description',
    enableColumnFilter: false,
  },
  {
    id: 'actions',
    cell: ActionCell,
    enableColumnFilter: false,
  },
];

const ProductSecrets = () => {
  const params = Route.useParams();
  const search = Route.useSearch();
  const pageIndex = search.page ? search.page - 1 : 0;
  const pageSize = search.pageSize ? search.pageSize : defaultPageSize;

  const {
    data: product,
    error: prodError,
    isPending: prodIsPending,
    isError: prodIsError,
  } = useProductsServiceGetApiV1ProductsByProductId({
    productId: Number.parseInt(params.productId),
  });

  const {
    data: secrets,
    error: secretsError,
    isPending: secretsIsPending,
    isError: secretsIsError,
  } = useProductsServiceGetApiV1ProductsByProductIdSecrets({
    productId: Number(params.productId),
    limit: pageSize,
    offset: pageIndex * pageSize,
  });

  const table = useReactTable({
    data: secrets?.data || [],
    columns,
    pageCount: Math.ceil((secrets?.pagination.totalCount ?? 0) / pageSize),
    state: {
      pagination: {
        pageIndex,
        pageSize,
      },
    },
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
  });

  if (prodIsPending || secretsIsPending) {
    return <LoadingIndicator />;
  }

  if (prodIsError || secretsIsError) {
    toast.error('Unable to load data', {
      description: <ToastError error={prodError || secretsError} />,
      duration: Infinity,
      cancel: {
        label: 'Dismiss',
        onClick: () => {},
      },
    });
    return;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Secrets</CardTitle>
        <CardDescription>Manage secrets for {product.name}.</CardDescription>
        <div className='py-2'>
          <Tooltip>
            <TooltipTrigger asChild>
              <Button asChild size='sm' className='ml-auto gap-1'>
                <Link
                  to='/organizations/$orgId/products/$productId/secrets/create-secret'
                  params={{
                    orgId: params.orgId,
                    productId: params.productId,
                  }}
                >
                  New secret
                  <PlusIcon className='h-4 w-4' />
                </Link>
              </Button>
            </TooltipTrigger>
            <TooltipContent>
              Create a new secret for this product
            </TooltipContent>
          </Tooltip>
        </div>
      </CardHeader>
      <CardContent>
        <DataTable
          table={table}
          setCurrentPageOptions={(currentPage) => {
            return {
              to: Route.to,
              search: { ...search, page: currentPage },
            };
          }}
          setPageSizeOptions={(size) => {
            return {
              to: Route.to,
              search: { ...search, page: 1, pageSize: size },
            };
          }}
        />
      </CardContent>
    </Card>
  );
};

export const Route = createFileRoute(
  '/organizations/$orgId/products/$productId/secrets/'
)({
  validateSearch: paginationSearchParameterSchema,
  loaderDeps: ({ search: { page, pageSize } }) => ({ page, pageSize }),
  loader: async ({ context, params, deps: { page, pageSize } }) => {
    await Promise.allSettled([
      prefetchUseProductsServiceGetApiV1ProductsByProductId(
        context.queryClient,
        {
          productId: Number.parseInt(params.productId),
        }
      ),
      prefetchUseProductsServiceGetApiV1ProductsByProductIdSecrets(
        context.queryClient,
        {
          productId: Number(params.productId),
          limit: pageSize || defaultPageSize,
          offset: page ? (page - 1) * (pageSize || defaultPageSize) : 0,
        }
      ),
    ]);
  },
  component: ProductSecrets,
  pendingComponent: LoadingIndicator,
});
