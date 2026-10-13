# Contributing Guidelines

Thank you for contributing to the VietBlog project! To maintain a clean, readable, and professional repository, please adhere to the following guidelines when making changes.

## 1. Branching Strategy (Git Flow)

We follow the standard Git Flow model:
- `main`: Production-ready code only.
- `dev`: The main integration branch. All features are merged here before release.
- `feature/*`: New features (e.g., `feature/comments`, `feature/auth`).
- `hotfix/*`: Quick fixes for production issues.

**Rule:** Never push directly to `main` or `dev`. Always create a new branch from `dev`, make your changes, and submit a Pull Request.

## 2. Commit Message Convention

We strictly follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. **All commit messages MUST be in English.**

### Format:
```
<type>(<scope>): <short description>
```

### Types:
- `feat`: A new feature (e.g., `feat(auth): implement JWT login`)
- `fix`: A bug fix (e.g., `fix(category): resolve null pointer exception in slug generation`)
- `docs`: Documentation changes (e.g., `docs: update README with setup instructions`)
- `refactor`: A code change that neither fixes a bug nor adds a feature (e.g., `refactor(domain): move entities to entity package`)
- `style`: Changes that do not affect the meaning of the code (white-space, formatting, etc)
- `test`: Adding missing tests or correcting existing tests
- `chore`: Changes to the build process or auxiliary tools

## 3. Pull Request (PR) Process

Once your feature or fix is complete, follow these steps to merge it into `dev`:

1. **Push your branch** to GitHub.
2. **Create a Pull Request** targeting the `dev` branch.
3. **Title:** Must follow the commit message convention (e.g., `feat: Add Comment CRUD API`).
4. **Description:** Please use the following template for your PR description:

```markdown
## Description
Provide a brief description of what this PR does.
- Added CommentDomainService
- Added API endpoints for Comments

## Related Issues
Link any related issues here (e.g., Fixes #12)

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [x] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
```

5. **Review:** Wait for approval from the team lead or reviewers before merging.

*Happy Coding!* 🚀
