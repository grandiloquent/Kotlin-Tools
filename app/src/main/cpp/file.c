#include "file.h"


void insert_node(struct DirNode **head, const char *path, long total) {
    struct DirNode *new_node = (struct DirNode *) malloc(sizeof(struct DirNode));
    struct DirNode *p, *q;
    if (!new_node) {
        return;
    }
    new_node->path = path;
    new_node->total = total;

    if (*head == NULL) {
        new_node->next = NULL;
        new_node->prev = NULL;
        *head = new_node;
    } else {
        p = *head;
        while (total < p->total) {
            q = p;
            p = p->next;
        }
        new_node->next = p;
        new_node->prev = q;
        if (p) {
            p->prev = new_node;
        }
    }
}

LIST *list_directories(const char *path) {
    int list_len = 2;
    LIST *head = NULL, *current = NULL;
    DIR *dir = opendir(path);
    if (dir) {
        size_t path_len = strlen(path);
        struct dirent *ent;
        int c = 0, cur = 0;
        while ((ent = readdir(dir)) != NULL) {
            if (c < 2 && (!strcmp(".", ent->d_name) || !strcmp("..", ent->d_name))) {
                c++;
                continue;
            }
            char *buf;
            size_t len;
            len = path_len + strlen(ent->d_name) + 2;
            buf = malloc(len);
            if (buf) {
                struct stat st;
                if (path[path_len - 1] == PATH_SEPARATOR)
                    snprintf(buf, len, "%s%s", path, ent->d_name);
                else
                    snprintf(buf, len, "%s%c%s", path, PATH_SEPARATOR, ent->d_name);

                if (!stat(buf, &st) && S_ISDIR(st.st_mode)) {
                    LIST *node = (LIST *) malloc(sizeof(LIST));
                    node->n = cur;
                    node->line = buf;
                    node->next = NULL;
                    if (head == NULL) {
                        current = head = node;
                    } else {
                        current = current->next = node;
                    }
                    cur++;
                }
            }
        }
        closedir(dir);
    }
    return head;
}

char *readable_size(double size, char *buf) {
    /*
     char total_buf[10];
  readable_size(total, total_buf);
  */
    int i = 0;
    const char *units[] = {"B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    while (size > 1024) {
        size /= 1024;
        i++;
    }
    sprintf(buf, "%.*f %s", i, size, units[i]);
    return buf;
}

void calculate_files_recursively(const char *path, size_t *total) {
    DIR *dir;
    struct dirent *ent;
    struct stat st;
    dir = opendir(path);
    int c = 0;
    size_t path_len = strlen(path);
    if (dir) {
        while ((ent = readdir(dir)) != NULL) {
            if (c < 2 && (!strcmp(".", ent->d_name) || !strcmp("..", ent->d_name))) {
                c++;
                continue;
            }
            size_t len = path_len + 2 + strlen(ent->d_name);
            char fp[len];
            snprintf(fp, len, "%s%c%s", path, PATH_SEPARATOR, ent->d_name);
            if (stat(fp, &st) == 0) {
                if (S_ISREG(st.st_mode)) {
                    *total += st.st_size;
                } else {
                    calculate_files_recursively(fp, total);
                }
            }
        }
        closedir(dir);
    }


}

int remove_directory(const char *path) {
    if (file_exists(path)) {
        unlink(path);
        return -1;
    }
    DIR *dir;
    dir = opendir(path);
    size_t path_len = strlen(path);
    int r = -1;
    if (dir) {
        struct dirent *p;
        r = 0;
        while (!r && (p = readdir(dir))) {
            int r2 = -1;
            char *buf;
            size_t len;
            if (!strcmp(p->d_name, ".") || !strcmp(p->d_name, "..")) continue;
            len = path_len + strlen(p->d_name) + 2;
            buf = malloc(len);
            if (buf) {
                struct stat st;
                snprintf(buf, len, "%s/%s", path, p->d_name);
                if (!stat(buf, &st)) {
                    if (S_ISDIR(st.st_mode)) {
                        r2 = remove_directory(buf);
                    } else {
                        r2 = unlink(buf);
                    }
                }
                free(buf);

            }
            r = r2;
        }
        closedir(dir);
    }
    if (!r) {
        r = rmdir(path);
    }
    return r;
}

void LIST_free(LIST *head) {
    while (head) {
        LIST *tmp = head;
        head = head->next;
        free(tmp->line);
        free(tmp);
    }
}

bool file_exists(const char *path) {
    struct stat st;
    return (!stat(path, &st) && S_ISREG(st.st_mode));
}