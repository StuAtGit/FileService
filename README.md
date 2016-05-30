#FileService

This is a web service for managing a users files. It depends on an auth service to validate tokens,
and a UserItemManager library to persist and retrieve the files.
This is part of a refactoring to break up the current SharePlayLearn api into smaller, more manageable services.
It will also likely depend on a separate UserItem library that will be broken out of the UserItem + UserItemManager models in the current API.

Note that unless specified otherwise, all copyrightable material in any git repository managed by me (Stuart C Smith) is licensed under the GPLv3