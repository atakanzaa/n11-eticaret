import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
  computed,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { CategoryApi } from '@core/api/category.api';
import {
  CategoryResponse,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from '@core/models/category.types';
import { ToastService } from '@core/toast.service';
import { TPipe } from '@shared/i18n.pipe';
import { SpinnerComponent } from '@shared/ui/spinner/spinner.component';
import { EmptyStateComponent } from '@shared/ui/empty-state/empty-state.component';
import { StatusBadgeComponent } from '@shared/ui/status-badge/status-badge.component';
import { ModalComponent } from '@shared/ui/modal/modal.component';
import { ConfirmDialogComponent } from '@shared/ui/confirm-dialog/confirm-dialog.component';
import { FormFieldComponent } from '@shared/ui/form-field/form-field.component';

interface CategoryNode {
  category: CategoryResponse;
  children: CategoryNode[];
  expanded: boolean;
}

@Component({
  selector: 'sc-admin-categories',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    TPipe,
    SpinnerComponent,
    EmptyStateComponent,
    StatusBadgeComponent,
    ModalComponent,
    ConfirmDialogComponent,
    FormFieldComponent,
  ],
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.scss'],
})
export class AdminCategoriesComponent implements OnInit {
  private readonly categoryApi = inject(CategoryApi);
  private readonly toast = inject(ToastService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly categories = signal<CategoryResponse[]>([]);

  // Build tree structure
  readonly tree = computed<CategoryNode[]>(() => {
    const cats = this.categories();
    const map = new Map<string, CategoryNode>();
    const roots: CategoryNode[] = [];

    // First pass: create nodes
    for (const cat of cats) {
      map.set(cat.id, { category: cat, children: [], expanded: true });
    }

    // Second pass: build tree
    for (const cat of cats) {
      const node = map.get(cat.id)!;
      if (cat.parentId && map.has(cat.parentId)) {
        map.get(cat.parentId)!.children.push(node);
      } else {
        roots.push(node);
      }
    }

    return roots;
  });

  // Only top-level categories (for parent dropdown)
  readonly topLevelCategories = computed(() =>
    this.categories().filter(c => !c.parentId)
  );

  // Modal state
  readonly showCreateModal = signal(false);
  readonly showEditModal = signal(false);
  readonly showDeleteConfirm = signal(false);
  readonly editingCategory = signal<CategoryResponse | null>(null);
  readonly deletingCategory = signal<CategoryResponse | null>(null);

  // Create form
  createForm: CreateCategoryRequest = {
    parentId: undefined,
    name: '',
    slug: '',
    description: '',
    imageUrl: '',
    displayOrder: 0,
  };

  // Edit form
  editForm: UpdateCategoryRequest = {};

  async ngOnInit(): Promise<void> {
    await this.refresh();
  }

  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      const list = await firstValueFrom(this.categoryApi.listForAdmin());
      this.categories.set(list);
    } finally {
      this.loading.set(false);
    }
  }

  // ── Slug auto-generate ────────────────────────
  generateSlug(name: string): string {
    return name
      .toLowerCase()
      .replace(/ş/g, 's')
      .replace(/ç/g, 'c')
      .replace(/ğ/g, 'g')
      .replace(/ı/g, 'i')
      .replace(/ö/g, 'o')
      .replace(/ü/g, 'u')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  }

  onCreateNameChange(name: string): void {
    this.createForm.name = name;
    this.createForm.slug = this.generateSlug(name);
  }

  // ── Create ────────────────────────────────────
  openCreateModal(parentId?: string): void {
    const maxOrder = Math.max(0, ...this.categories().map(c => c.displayOrder));
    this.createForm = {
      parentId: parentId,
      name: '',
      slug: '',
      description: '',
      imageUrl: '',
      displayOrder: maxOrder + 1,
    };
    this.showCreateModal.set(true);
  }

  async submitCreate(): Promise<void> {
    this.saving.set(true);
    try {
      await firstValueFrom(this.categoryApi.create(this.createForm));
      this.toast.show('Kategori oluşturuldu', 'success');
      this.showCreateModal.set(false);
      await this.refresh();
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.saving.set(false);
    }
  }

  // ── Edit ──────────────────────────────────────
  openEditModal(cat: CategoryResponse): void {
    this.editingCategory.set(cat);
    this.editForm = {
      parentId: cat.parentId,
      name: cat.name,
      slug: cat.slug,
      description: cat.description ?? '',
      imageUrl: cat.imageUrl ?? '',
      displayOrder: cat.displayOrder,
      active: cat.active ?? true,
    };
    this.showEditModal.set(true);
  }

  async submitEdit(): Promise<void> {
    const cat = this.editingCategory();
    if (!cat) return;
    this.saving.set(true);
    try {
      await firstValueFrom(this.categoryApi.update(cat.id, this.editForm));
      this.toast.show('Kategori güncellendi', 'success');
      this.showEditModal.set(false);
      await this.refresh();
    } catch {
      /* error.interceptor handles toast */
    } finally {
      this.saving.set(false);
    }
  }

  // ── Delete ────────────────────────────────────
  openDeleteConfirm(cat: CategoryResponse): void {
    this.deletingCategory.set(cat);
    this.showDeleteConfirm.set(true);
  }

  async confirmDelete(): Promise<void> {
    const cat = this.deletingCategory();
    if (!cat) return;
    try {
      await firstValueFrom(this.categoryApi.delete(cat.id));
      this.toast.show('Kategori silindi', 'success');
      this.showDeleteConfirm.set(false);
      await this.refresh();
    } catch {
      /* error.interceptor handles toast */
    }
  }

  cancelDelete(): void {
    this.showDeleteConfirm.set(false);
    this.deletingCategory.set(null);
  }

  // ── Toggle active status ──────────────────────
  async toggleActive(cat: CategoryResponse): Promise<void> {
    try {
      await firstValueFrom(this.categoryApi.update(cat.id, { active: !(cat.active ?? true) }));
      this.toast.show(
        cat.active ? 'Kategori pasif yapıldı' : 'Kategori aktif yapıldı',
        'success'
      );
      await this.refresh();
    } catch {
      /* error.interceptor handles toast */
    }
  }

  // ── Helpers ───────────────────────────────────
  activeVariant(cat: CategoryResponse): 'success' | 'neutral' {
    return (cat.active ?? true) ? 'success' : 'neutral';
  }

  activeLabel(cat: CategoryResponse): string {
    return (cat.active ?? true) ? 'Aktif' : 'Pasif';
  }
}
