#include "file.h"

const char INVALID_FILE_CHARS[] = {
        '"', '<', '>', '|', '\0', '\x0001', '\x0002',
        '\x0003', '\x0004', '\x0005', '\x0006', '\a', '\b', '\t',
        '\n', '\v', '\f', '\r', '\x000e', '\x000f', '\x0010',
        '\x0011', '\x0012', '\x0013', '\x0014', '\x0015', '\x0016', '\x0017',
        '\x0018', '\x0019', '\x001a', '\x001b', '\x001c', '\x001d', '\x001e',
        '\x001f', ':', '*', '?', '\\', '/'};

char *GetValidFileName(const char *path) {

    size_t len = sizeof(INVALID_FILE_CHARS) / sizeof(char);
    char *tmp = (char *) path;
    char *r = malloc(sizeof(char) * (strlen(tmp) + 1));
    char *fr = r;
    while (*tmp) {
        *r = *tmp;
        for (size_t i = 0; i < len; i++) {
            if (*tmp == INVALID_FILE_CHARS[i]) {
                *r = ' ';
            }

        }
        ++tmp;
        ++r;
    }
    *r = '\0';
    return fr;
}

char *SubStringBeforeLast(const char *str, char c) {
    size_t len = strlen(str);
    char *s = malloc(sizeof(char) * (len + 1));
    (void) memcpy(s, str, len);
    char n;
    int i = 0, j = 0;

    while ((n = *str++) != 0) {
        if (n == c) j = i;
        i++;
    }
    *(s + j) = '\0';
    return s;
}

char *SubStringAfterLast(const char *str, char c) {
    char *s = (char *) str;
    char n;
    int i = 0, j = 0;

    while ((n = *str++) != 0) {
        if (n == c) j = i;
        i++;
    }
    return s + j + 1;
}

void RenameMp3File(const char *path) {
    char buf_title[31];
    char buf_artist[31];
    FILE *file = fopen(path, "rb");
    fseek(file, -125, SEEK_END);
    fread(buf_title, 1, 30, file);
    if (strlen(buf_title) == 0)
        return;
    fread(buf_artist, 1, 30, file);

    char *dir = SubStringBeforeLast(path, PATH_SEPARATOR);
    char *ext = SubStringAfterLast(path, '.');
    const char *sep = " - ";
    size_t len =
            strlen(dir) + strlen(ext) + strlen(buf_title) + strlen(buf_artist) + strlen(sep) + 2;
    char *targetFileName = malloc(sizeof(char) * len);
    snprintf(targetFileName, len, "%s%c%s%s%s%s", dir, PATH_SEPARATOR, buf_title, sep, buf_artist,
             ext);
    rename(path, targetFileName);
    free(dir);
    free(targetFileName);
    fclose(file);
}

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