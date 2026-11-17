export const SOCIAL_OPPORTUNITY_ID_PREFIX = 'social-'

export function mapSocialOpportunityToJob(post) {
  if (!post || post.id == null) return null

  const details = post.opportunityDetails && typeof post.opportunityDetails === 'object'
    ? post.opportunityDetails
    : {}
  const title = post.title || details.projectName || 'Film opportunity'
  const date = post.createdAt ? new Date(post.createdAt) : null
  const postedDate = date && !Number.isNaN(date.getTime())
    ? date.toLocaleDateString()
    : 'Recently'
  const description = post.content || [
    details.projectType,
    details.actingRole,
    details.locationPreference,
  ].filter(Boolean).join(' · ')

  return {
    id: `${SOCIAL_OPPORTUNITY_ID_PREFIX}${post.id}`,
    socialPostId: String(post.id),
    source: 'social',
    title,
    roleCategory: details.craft || details.actingRole || details.projectType || 'Film opportunity',
    productionCompany: details.productionCompany || details.companyName || post.authorName || 'Community listing',
    location: details.locationPreference || 'Location not specified',
    type: details.projectType || 'Social opportunity',
    budgetOrPay: details.budgetOrPay || details.budget || details.pay || '',
    postedDate,
    deadline: details.applicationDeadline || details.auditionDate || 'Not specified',
    description: description || 'No description provided.',
    requirements: [],
  }
}
