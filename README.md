#FileService

This is a web service for managing a users files. It depends on an auth service to validate tokens.
This is part of a refactoring to break up the current SharePlayLearn api into smaller, more manageable services.
It will also likely depend on a separate UserItem library that will be broken out of the UserItem + UserItemManager models in the current API.