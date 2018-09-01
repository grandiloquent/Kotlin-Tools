
#ifndef TOOLS_FILE_H
#define TOOLS_FILE_H

#endif //TOOLS_FILE_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <sys/stat.h>
#include <stdbool.h>

#if defined(WIN32) || defined(_WIN32)
#define PATH_SEPARATOR '\\'
#else
#define PATH_SEPARATOR '/'
#endif


struct DirNode {
    const char *path;
    long total;
    struct DirNode *prev;
    struct DirNode *next;
};

struct list {
    char *line;
    int n;
    struct list *next;
};
typedef struct list LIST;

void insert_node(struct DirNode **head, const char *path, long total);

LIST *list_directories(const char *path);

void calculate_files_recursively(const char *path, size_t *total);

void LIST_free(LIST *head);

int remove_directory(const char *path);

char *readable_size(double size, char *buf);

bool file_exists(const char *);
